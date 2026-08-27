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

package uk.gov.hmrc.vapingduty.scheduling

import org.apache.pekko.actor.ActorSystem
import play.api.{Configuration, Logging}
import uk.gov.hmrc.vapingduty.services.NrsService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class NrsScheduledService @Inject() (
  actorSystem: ActorSystem,
  nrsService: NrsService,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends Logging {

  private val enabled: Boolean = configuration.get[Boolean]("nrs-submission-scheduler.enabled")
  private val initialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("nrs-submission-scheduler.initial-delay")
  private val interval: FiniteDuration = configuration.get[FiniteDuration]("nrs-submission-scheduler.interval")

  if (enabled) {
    logger.info(
      s"NRS submission scheduler enabled - initial delay: $initialDelay, interval: $interval"
    )
    scheduleProcessing()
  } else {
    logger.info("NRS submission scheduler disabled")
  }

  private def scheduleProcessing(): Unit =
    actorSystem.scheduler.scheduleAtFixedRate(
      initialDelay = initialDelay,
      interval = interval
    ) { () =>
      logger.debug("NRS submission scheduler triggered")
      nrsService.processAll().recover { case ex =>
        logger.error("Error processing NRS work items", ex)
      }
    }
}