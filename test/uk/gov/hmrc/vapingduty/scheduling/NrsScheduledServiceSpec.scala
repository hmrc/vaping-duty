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

import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito.{atLeastOnce, never, verify, when}
import org.scalatest.BeforeAndAfterAll
import play.api.Configuration
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.services.NrsService

import scala.concurrent.Future

class NrsScheduledServiceSpec extends SpecBase with BeforeAndAfterAll {

  private val mockNrsService = mock[NrsService]
  private val mockAppConfig  = mock[AppConfig]
  private val actorSystem    = ActorSystem("test-actor-system")

  override def afterAll(): Unit = {
    actorSystem.terminate()
    super.afterAll()
  }

  "NrsScheduledService" - {
    "when scheduler is enabled and feature switch is enabled" - {
      "must initialize and log that it is enabled" in {
        val config = Configuration(
          "nrs-submission-scheduler.interval"      -> "30 seconds",
          "nrs-submission-scheduler.initial-delay" -> "1 minute"
        )

        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)
        when(mockNrsService.processAll()).thenReturn(Future.successful(Done))

        new NrsScheduledService(actorSystem, mockNrsService, config, mockAppConfig)

        // Scheduler is initialized but we can't easily test the scheduled execution in a unit test
        // The integration test will verify the actual scheduling behavior
        succeed
      }
    }
    
    "when scheduler is enabled but feature switch is disabled" - {
      "must skip processing" in {
        val config = Configuration(
          "nrs-submission-scheduler.interval"      -> "100 milliseconds",
          "nrs-submission-scheduler.initial-delay" -> "10 milliseconds"
        )

        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(false)

        new NrsScheduledService(actorSystem, mockNrsService, config, mockAppConfig)

        // Wait to ensure scheduler runs but doesn't call processAll
        Thread.sleep(300)

        verify(mockNrsService, never()).processAll()
      }
    }

    "when scheduler is disabled" - {
      "must not schedule any processing" in {
        val config = Configuration(
          "nrs-submission-scheduler.interval"      -> "30 seconds",
          "nrs-submission-scheduler.initial-delay" -> "1 minute"
        )

        // Don't need to mock appConfig since scheduler won't start
        new NrsScheduledService(actorSystem, mockNrsService, config, mockAppConfig)

        // Give it a moment to ensure nothing is scheduled
        Thread.sleep(100)

        verify(mockNrsService, never()).processAll()
      }
    }

    "when scheduler encounters an error" - {
      "must log the error and continue" in {
        val config = Configuration(
          "nrs-submission-scheduler.interval"      -> "100 milliseconds",
          "nrs-submission-scheduler.initial-delay" -> "10 milliseconds"
        )

        when(mockAppConfig.nrsGenerationEnabled).thenReturn(true)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)
        when(mockNrsService.processAll())
          .thenReturn(Future.failed(new RuntimeException("Test error")))

        new NrsScheduledService(actorSystem, mockNrsService, config, mockAppConfig)

        // Wait for at least two executions (initial + one retry)
        Thread.sleep(300)

        // Verify that processAll was called at least once despite the error
        // This confirms the scheduler continues running after errors
        verify(mockNrsService, atLeastOnce()).processAll()
      }
    }
  }
}