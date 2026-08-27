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
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.vapingduty.base.ISpecBase
import uk.gov.hmrc.vapingduty.models.nrs.{NrsPayload, NrsSubmissionWorkItem}
import uk.gov.hmrc.vapingduty.repositories.NrsWorkItemRepository
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import java.time.Instant
import scala.concurrent.duration._

class NrsScheduledServiceISpec extends ISpecBase with Eventually {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(1, Seconds))

  private val SCHEDULER_ENABLED_CONFIG = Configuration(
    "nrs-submission-scheduler.enabled"       -> true,
    "nrs-submission-scheduler.interval"      -> "1 second",
    "nrs-submission-scheduler.initial-delay" -> "500 milliseconds"
  )

  private val SCHEDULER_DISABLED_CONFIG = Configuration(
    "nrs-submission-scheduler.enabled"       -> false,
    "nrs-submission-scheduler.interval"      -> "1 second",
    "nrs-submission-scheduler.initial-delay" -> "500 milliseconds"
  )

  "NrsScheduledService" - {
    "when scheduler is enabled" - {
      "must process work items from the queue" in {
        val app = GuiceApplicationBuilder()
          .configure(SCHEDULER_ENABLED_CONFIG.underlying)
          .build()

        running(app) {
          val repository = app.injector.instanceOf[NrsWorkItemRepository]

          // Clear any existing items
          await(repository.collection.drop().toFuture())

          // Create a test work item
          val testPayload = NrsPayload(
            payload = "test-payload",
            metadata = testNrsMetadata
          )
          val workItem = NrsSubmissionWorkItem(testPayload)

          // Add work item to queue
          await(
            repository.pushNew(
              workItem,
              Instant.now(),
              ProcessingStatus.ToDo
            )
          )

          // Wait for scheduler to process the item
          // Note: In a real integration test with WireMock, we would stub the NRS endpoint
          // For now, we just verify the scheduler attempts to process items
          eventually {
            val items = await(repository.collection.find().toFuture())
            // The item should either be processed or marked as failed (since NRS endpoint is not stubbed)
            items.headOption.map(_.status) should not be Some(ProcessingStatus.ToDo)
          }
        }
      }

      "must continue processing after encountering errors" in {
        val app = GuiceApplicationBuilder()
          .configure(SCHEDULER_ENABLED_CONFIG.underlying)
          .build()

        running(app) {
          val repository = app.injector.instanceOf[NrsWorkItemRepository]

          // Clear any existing items
          await(repository.collection.drop().toFuture())

          // Create multiple test work items
          val testPayload1 = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload-1", metadata = testNrsMetadata)
          )
          val testPayload2 = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload-2", metadata = testNrsMetadata)
          )

          await(repository.pushNew(testPayload1, Instant.now(), ProcessingStatus.ToDo))
          await(repository.pushNew(testPayload2, Instant.now(), ProcessingStatus.ToDo))

          // Wait for scheduler to attempt processing
          eventually {
            val items = await(repository.collection.find().toFuture())
            // Both items should have been attempted
            items.size shouldBe 2
            items.forall(_.status != ProcessingStatus.ToDo) shouldBe true
          }
        }
      }
    }

    "when scheduler is disabled" - {
      "must not process work items from the queue" in {
        val app = GuiceApplicationBuilder()
          .configure(SCHEDULER_DISABLED_CONFIG.underlying)
          .build()

        running(app) {
          val repository = app.injector.instanceOf[NrsWorkItemRepository]

          // Clear any existing items
          await(repository.collection.drop().toFuture())

          // Create a test work item
          val testPayload = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload", metadata = testNrsMetadata)
          )

          await(
            repository.pushNew(
              testPayload,
              Instant.now(),
              ProcessingStatus.ToDo
            )
          )

          // Wait a reasonable time
          Thread.sleep(2000)

          // Item should still be in ToDo status
          val items = await(repository.collection.find().toFuture())
          items.headOption.map(_.status) shouldBe Some(ProcessingStatus.ToDo)
        }
      }
    }
  }
}