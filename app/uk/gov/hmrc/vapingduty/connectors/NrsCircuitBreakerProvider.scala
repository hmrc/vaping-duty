/*
 * Copyright 2026 HM Revenue & Customs
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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import play.api.Logging
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.connectors.NrsConnector.NrsCircuitBreaker

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class NrsCircuitBreakerProvider @Inject()(
                                           appConfig: AppConfig,
                                           system: ActorSystem
                                         )(implicit ec: ExecutionContext)
  extends Provider[NrsCircuitBreaker] with Logging {

  private val maxFailures: Int                  = appConfig.nrsCircuitBreakerMaxFailures
  private val callTimeout: FiniteDuration       = appConfig.nrsCircuitBreakerCallTimeout
  private val resetTimeout: FiniteDuration      = appConfig.nrsCircuitBreakerResetTimeout
  private val maxResetTimeout: FiniteDuration   = appConfig.nrsCircuitBreakerMaxResetTimeout
  private val exponentialBackoffFactor: Double  = appConfig.nrsCircuitBreakerExponentialBackoffFactor

  private val breaker: CircuitBreaker =
    new CircuitBreaker(
      scheduler = system.scheduler,
      maxFailures = maxFailures, // how many times it fails before it trips the breaker and sets state to half-open
      callTimeout = callTimeout, // how long it waits before it deems a call failed
      resetTimeout = resetTimeout, // this is how long it waits before it tries another call
      maxResetTimeout = maxResetTimeout, // maximum interval to back off to
      exponentialBackoffFactor = exponentialBackoffFactor // factor by which to ramp the backoff
    )
      .onOpen {
        logger.warn("NRS Circuit Breaker has opened")
      }
      .onHalfOpen {
        logger.warn("NRS Circuit Breaker set to half open")
      }
      .onClose {
        logger.info("NRS Circuit Breaker has closed")
      }

  override def get(): NrsCircuitBreaker = NrsCircuitBreaker(breaker)
}
