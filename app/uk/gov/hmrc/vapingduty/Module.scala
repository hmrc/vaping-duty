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

package uk.gov.hmrc.vapingduty

import com.google.inject.{AbstractModule, Provides}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import play.api.Configuration
import uk.gov.hmrc.vapingduty.connectors.NrsCircuitBreaker
import uk.gov.hmrc.vapingduty.controllers.actions.{AuthorisedAction, BaseAuthorisedAction}
import uk.gov.hmrc.vapingduty.scheduling.NrsScheduledService

import java.time.Clock
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    bind(classOf[AuthorisedAction]).to(classOf[BaseAuthorisedAction])
    bind(classOf[NrsScheduledService]).asEagerSingleton()
  }

  @Provides
  @Singleton
  def provideNrsCircuitBreaker(
    actorSystem: ActorSystem,
    configuration: Configuration
  )(implicit ec: ExecutionContext): NrsCircuitBreaker = {
    val maxFailures  = configuration.get[Int]("microservice.services.nrs.circuit-breaker.max-failures")
    val callTimeout  = configuration.get[FiniteDuration]("microservice.services.nrs.circuit-breaker.call-timeout")
    val resetTimeout = configuration.get[FiniteDuration]("microservice.services.nrs.circuit-breaker.reset-timeout")

    NrsCircuitBreaker(
      new CircuitBreaker(
        actorSystem.scheduler,
        maxFailures = maxFailures,
        callTimeout = callTimeout,
        resetTimeout = resetTimeout
      )
    )
  }
}
