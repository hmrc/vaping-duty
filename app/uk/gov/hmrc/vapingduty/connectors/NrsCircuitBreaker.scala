package uk.gov.hmrc.vapingduty.connectors

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.{Clock, Instant}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference}
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsCircuitBreaker @Inject() (
  metrics: Metrics,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends Logging {

  private val CIRCUIT_BREAKER_PREFIX = "nrs.circuit-breaker"
  private val METRIC_STATE           = s"$CIRCUIT_BREAKER_PREFIX.state"
  private val METRIC_FAILURES        = s"$CIRCUIT_BREAKER_PREFIX.failures"
  private val METRIC_SUCCESSES       = s"$CIRCUIT_BREAKER_PREFIX.successes"
  private val METRIC_CALLS_REJECTED  = s"$CIRCUIT_BREAKER_PREFIX.calls-rejected"
  private val METRIC_STATE_CHANGES   = s"$CIRCUIT_BREAKER_PREFIX.state-changes"

  private val MAX_FAILURES: Int                      = 5
  private val RESET_TIMEOUT: FiniteDuration          = 60.seconds
  private val HALF_OPEN_MAX_CALLS: Int               = 3
  private val EXPONENTIAL_BACKOFF_MULTIPLIER: Double = 2.0
  private val MAX_RESET_TIMEOUT: FiniteDuration      = 300.seconds

  sealed trait CircuitBreakerState
  case object Closed extends CircuitBreakerState
  case object Open extends CircuitBreakerState
  case object HalfOpen extends CircuitBreakerState

  private val state: AtomicReference[CircuitBreakerState] = new AtomicReference(Closed)
  private val failureCount: AtomicInteger                 = new AtomicInteger(0)
  private val consecutiveSuccesses: AtomicInteger         = new AtomicInteger(0)
  private val halfOpenCallCount: AtomicInteger            = new AtomicInteger(0)
  private val openedAt: AtomicLong                        = new AtomicLong(0)
  private val currentResetTimeout: AtomicReference[FiniteDuration] =
    new AtomicReference(RESET_TIMEOUT)

  // Initialize metrics
  updateStateMetric(Closed)

  def withCircuitBreaker[T](operation: => Future[T]): Future[T] =
    state.get() match {
      case Closed =>
        executeInClosed(operation)

      case Open =>
        if (shouldAttemptReset()) {
          transitionToHalfOpen()
          executeInHalfOpen(operation)
        } else {
          recordCallRejected()
          Future.failed(
            new RuntimeException(
              s"Circuit breaker is OPEN. Opened at ${Instant.ofEpochMilli(openedAt.get())}. Will retry after ${currentResetTimeout.get()}"
            )
          )
        }

      case HalfOpen =>
        executeInHalfOpen(operation)
    }

  private def executeInClosed[T](operation: => Future[T]): Future[T] =
    operation.transform { result =>
      result match {
        case scala.util.Success(_) =>
          onSuccess()
          result
        case scala.util.Failure(ex) =>
          onFailure(ex)
          result
      }
    }

  private def executeInHalfOpen[T](operation: => Future[T]): Future[T] = {
    val callNumber = halfOpenCallCount.incrementAndGet()

    if (callNumber > HALF_OPEN_MAX_CALLS) {
      halfOpenCallCount.decrementAndGet()
      recordCallRejected()
      Future.failed(
        new RuntimeException(
          "Circuit breaker is HALF_OPEN and max concurrent calls reached"
        )
      )
    } else {
      operation.transform { result =>
        halfOpenCallCount.decrementAndGet()
        result match {
          case scala.util.Success(_) =>
            onSuccessInHalfOpen()
            result
          case scala.util.Failure(ex) =>
            onFailureInHalfOpen(ex)
            result
        }
      }
    }
  }

  private def onSuccess(): Unit = {
    val failures = failureCount.get()
    if (failures > 0) {
      failureCount.set(0)
      logger.info(s"Circuit breaker reset failure count after success (was $failures)")
    }
    recordSuccess()
  }

  private def onFailure(ex: Throwable): Unit = {
    val failures = failureCount.incrementAndGet()
    recordFailure()

    logger.warn(s"Circuit breaker recorded failure ($failures/$MAX_FAILURES): ${ex.getMessage}")

    if (failures >= MAX_FAILURES) {
      transitionToOpen()
    }
  }

  private def onSuccessInHalfOpen(): Unit = {
    val successes = consecutiveSuccesses.incrementAndGet()
    recordSuccess()

    logger.info(
      s"Circuit breaker recorded success in HALF_OPEN state ($successes/$HALF_OPEN_MAX_CALLS)"
    )

    if (successes >= HALF_OPEN_MAX_CALLS) {
      transitionToClosed()
    }
  }

  private def onFailureInHalfOpen(ex: Throwable): Unit = {
    recordFailure()
    logger.warn(s"Circuit breaker recorded failure in HALF_OPEN state: ${ex.getMessage}")
    transitionToOpen()
  }

  private def transitionToOpen(): Unit = {
    val previousState = state.getAndSet(Open)
    if (previousState != Open) {
      val now = clock.instant().toEpochMilli
      openedAt.set(now)

      val newTimeout = calculateNextTimeout()
      currentResetTimeout.set(newTimeout)

      recordStateChange(previousState, Open)
      updateStateMetric(Open)

      logger.error(
        s"Circuit breaker transitioned from $previousState to OPEN at ${Instant.ofEpochMilli(now)}. " +
          s"Will attempt reset after $newTimeout. Failure count: ${failureCount.get()}"
      )
    }
  }

  private def transitionToHalfOpen(): Unit = {
    val previousState = state.getAndSet(HalfOpen)
    if (previousState != HalfOpen) {
      consecutiveSuccesses.set(0)
      halfOpenCallCount.set(0)

      recordStateChange(previousState, HalfOpen)
      updateStateMetric(HalfOpen)

      logger.info(
        s"Circuit breaker transitioned from $previousState to HALF_OPEN. " +
          s"Allowing up to $HALF_OPEN_MAX_CALLS test calls."
      )
    }
  }

  private def transitionToClosed(): Unit = {
    val previousState = state.getAndSet(Closed)
    if (previousState != Closed) {
      failureCount.set(0)
      consecutiveSuccesses.set(0)
      currentResetTimeout.set(RESET_TIMEOUT)

      recordStateChange(previousState, Closed)
      updateStateMetric(Closed)

      logger.info(
        s"Circuit breaker transitioned from $previousState to CLOSED. " +
          s"Reset timeout restored to $RESET_TIMEOUT."
      )
    }
  }

  private def shouldAttemptReset(): Boolean = {
    val now     = clock.instant().toEpochMilli
    val opened  = openedAt.get()
    val timeout = currentResetTimeout.get().toMillis
    now - opened >= timeout
  }

  private def calculateNextTimeout(): FiniteDuration = {
    val current = currentResetTimeout.get()
    val next    = (current.toMillis * EXPONENTIAL_BACKOFF_MULTIPLIER).toLong.millis
    if (next > MAX_RESET_TIMEOUT) MAX_RESET_TIMEOUT else next
  }

  private def recordSuccess(): Unit =
    metrics.defaultRegistry.counter(METRIC_SUCCESSES).inc()

  private def recordFailure(): Unit =
    metrics.defaultRegistry.counter(METRIC_FAILURES).inc()

  private def recordCallRejected(): Unit = {
    metrics.defaultRegistry.counter(METRIC_CALLS_REJECTED).inc()
    logger.warn("Circuit breaker rejected call - circuit is OPEN")
  }

  private def recordStateChange(from: CircuitBreakerState, to: CircuitBreakerState): Unit = {
    metrics.defaultRegistry.counter(METRIC_STATE_CHANGES).inc()
    logger.info(s"Circuit breaker state change recorded: $from -> $to")
  }

  private def updateStateMetric(newState: CircuitBreakerState): Unit = {
    val stateValue = newState match {
      case Closed   => 0
      case HalfOpen => 1
      case Open     => 2
    }
    metrics.defaultRegistry.gauge(METRIC_STATE, () => stateValue)
  }

  def getCurrentState: CircuitBreakerState = state.get()

  def getFailureCount: Int = failureCount.get()

  def getMetricsSnapshot: Map[String, Long] = Map(
    "state"          -> (getCurrentState match {
      case Closed   => 0L
      case HalfOpen => 1L
      case Open     => 2L
    }),
    "failureCount"   -> failureCount.get().toLong,
    "successes"      -> metrics.defaultRegistry.counter(METRIC_SUCCESSES).getCount,
    "failures"       -> metrics.defaultRegistry.counter(METRIC_FAILURES).getCount,
    "callsRejected"  -> metrics.defaultRegistry.counter(METRIC_CALLS_REJECTED).getCount,
    "stateChanges"   -> metrics.defaultRegistry.counter(METRIC_STATE_CHANGES).getCount
  )
}