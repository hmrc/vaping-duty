/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vapingduty.connectors

import com.codahale.metrics.MetricRegistry
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.vapingduty.base.SpecBase

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class NrsCircuitBreakerSpec extends SpecBase with ScalaFutures {

  private val fixedInstant = Instant.ofEpochMilli(1640000000000L)
  private val fixedClock   = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

  private val mockMetrics = new Metrics {
    override val defaultRegistry: MetricRegistry = new MetricRegistry()
  }

  "NrsCircuitBreaker" - {

    "must initialize in Closed state" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      circuitBreaker.getCurrentState mustBe circuitBreaker.Closed
      circuitBreaker.getFailureCount mustBe 0
    }

    "must record metrics on initialization" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      val snapshot = circuitBreaker.getMetricsSnapshot

      snapshot("state") mustBe 0L // Closed
    }

    "must transition to Open after max failures" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      // Trigger 5 failures
      (1 to 5).foreach { _ =>
        val result = circuitBreaker.withCircuitBreaker(
          Future.failed(new RuntimeException("Test failure"))
        )
        whenReady(result.failed) { _ =>
          // Failure recorded
        }
      }

      circuitBreaker.getCurrentState mustBe circuitBreaker.Open
      circuitBreaker.getFailureCount mustBe 5
    }

    "must record failure metrics" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      val result = circuitBreaker.withCircuitBreaker(
        Future.failed(new RuntimeException("Test failure"))
      )

      whenReady(result.failed) { _ =>
        val snapshot = circuitBreaker.getMetricsSnapshot
        snapshot("failures") mustBe 1L
      }
    }

    "must record success metrics" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      val result = circuitBreaker.withCircuitBreaker(
        Future.successful("success")
      )

      whenReady(result) { _ =>
        val snapshot = circuitBreaker.getMetricsSnapshot
        snapshot("successes") mustBe 1L
      }
    }

    "must reject calls when Open" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      // Trigger 5 failures to open circuit
      (1 to 5).foreach { _ =>
        val result = circuitBreaker.withCircuitBreaker(
          Future.failed(new RuntimeException("Test failure"))
        )
        whenReady(result.failed) { _ => }
      }

      // Next call should be rejected
      val rejectedResult = circuitBreaker.withCircuitBreaker(
        Future.successful("should not execute")
      )

      whenReady(rejectedResult.failed) { ex =>
        ex.getMessage must include("Circuit breaker is OPEN")

        val snapshot = circuitBreaker.getMetricsSnapshot
        snapshot("callsRejected") mustBe 1L
      }
    }

    "must record state change metrics" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      // Trigger transition to Open
      (1 to 5).foreach { _ =>
        val result = circuitBreaker.withCircuitBreaker(
          Future.failed(new RuntimeException("Test failure"))
        )
        whenReady(result.failed) { _ => }
      }

      val snapshot = circuitBreaker.getMetricsSnapshot
      snapshot("stateChanges") mustBe 1L // Closed -> Open
    }

    "must transition to HalfOpen after timeout" in {
      val variableClock = new java.time.Clock {
        private var currentTime = fixedInstant.toEpochMilli

        override def getZone: ZoneId = ZoneId.of("UTC")

        override def withZone(zone: ZoneId): Clock = this

        override def instant(): Instant = Instant.ofEpochMilli(currentTime)

        def advance(millis: Long): Unit = {
          currentTime += millis
        }
      }

      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, variableClock)

      // Open the circuit
      (1 to 5).foreach { _ =>
        val result = circuitBreaker.withCircuitBreaker(
          Future.failed(new RuntimeException("Test failure"))
        )
        whenReady(result.failed) { _ => }
      }

      circuitBreaker.getCurrentState mustBe circuitBreaker.Open

      // Advance time past reset timeout (60 seconds)
      variableClock.advance(61000)

      // Next call should transition to HalfOpen
      val result = circuitBreaker.withCircuitBreaker(
        Future.successful("test")
      )

      whenReady(result) { _ =>
        // State should have transitioned through HalfOpen
        val snapshot = circuitBreaker.getMetricsSnapshot
        snapshot("stateChanges") must be >= 2L // Closed->Open, Open->HalfOpen
      }
    }

    "must reset failure count on success in Closed state" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      // Record some failures
      (1 to 3).foreach { _ =>
        val result = circuitBreaker.withCircuitBreaker(
          Future.failed(new RuntimeException("Test failure"))
        )
        whenReady(result.failed) { _ => }
      }

      circuitBreaker.getFailureCount mustBe 3

      // Record a success
      val successResult = circuitBreaker.withCircuitBreaker(
        Future.successful("success")
      )

      whenReady(successResult) { _ =>
        circuitBreaker.getFailureCount mustBe 0
      }
    }

    "must provide metrics snapshot" in {
      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, fixedClock)

      // Record some activity
      val successResult = circuitBreaker.withCircuitBreaker(
        Future.successful("success")
      )
      whenReady(successResult) { _ => }

      val failureResult = circuitBreaker.withCircuitBreaker(
        Future.failed(new RuntimeException("failure"))
      )
      whenReady(failureResult.failed) { _ => }

      val snapshot = circuitBreaker.getMetricsSnapshot

      snapshot must contain key "state"
      snapshot must contain key "failureCount"
      snapshot must contain key "successes"
      snapshot must contain key "failures"
      snapshot must contain key "callsRejected"
      snapshot must contain key "stateChanges"

      snapshot("state") mustBe 0L // Closed
      snapshot("failureCount") mustBe 1L
      snapshot("successes") mustBe 1L
      snapshot("failures") mustBe 1L
    }

    "must use exponential backoff for reset timeout" in {
      val variableClock = new java.time.Clock {
        private var currentTime = fixedInstant.toEpochMilli

        override def getZone: ZoneId = ZoneId.of("UTC")

        override def withZone(zone: ZoneId): Clock = this

        override def instant(): Instant = Instant.ofEpochMilli(currentTime)

        def advance(millis: Long): Unit = {
          currentTime += millis
        }
      }

      val circuitBreaker = new NrsCircuitBreaker(mockMetrics, variableClock)

      // First opening - should use 60 second timeout
      (1 to 5).foreach { _ =>
        val result = circuitBreaker.withCircuitBreaker(
          Future.failed(new RuntimeException("Test failure"))
        )
        whenReady(result.failed) { _ => }
      }

      circuitBreaker.getCurrentState mustBe circuitBreaker.Open

      // Advance time and transition to HalfOpen
      variableClock.advance(61000)

      // Fail again to reopen with increased timeout
      val failResult = circuitBreaker.withCircuitBreaker(
        Future.failed(new RuntimeException("Test failure"))
      )
      whenReady(failResult.failed) { _ => }

      circuitBreaker.getCurrentState mustBe circuitBreaker.Open

      // Should now require 120 seconds (2x backoff)
      variableClock.advance(61000)

      val stillRejected = circuitBreaker.withCircuitBreaker(
        Future.successful("should be rejected")
      )

      whenReady(stillRejected.failed) { ex =>
        ex.getMessage must include("Circuit breaker is OPEN")
      }
    }
  }
}