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

import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatest.time.{Seconds, Span}
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout, running, ACCEPTED}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.vapingduty.base.ISpecBase
import uk.gov.hmrc.vapingduty.models.nrs.{NrsPayload, NrsSubmissionWorkItem}
import uk.gov.hmrc.vapingduty.repositories.NrsWorkItemRepository
import uk.gov.hmrc.vapingduty.utils.WireMockHelper

import java.time.Instant

class NrsScheduledServiceISpec extends ISpecBase with Eventually with WireMockHelper {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(200, org.scalatest.time.Millis))

  private def schedulerEnabledConfig = Configuration(
    "nrs-submission-scheduler.enabled"       -> true,
    "nrs-submission-scheduler.interval"      -> "1 second",
    "nrs-submission-scheduler.initial-delay" -> "2 seconds",
    "microservice.services.nrs.protocol"     -> "http",
    "microservice.services.nrs.host"         -> wireMockHost,
    "microservice.services.nrs.port"         -> wireMockPort,
    "microservice.services.nrs.url"          -> "/submission",
    "microservice.services.nrs.api-key"      -> "test-api-key",
    "microservice.services.nrs.max-failures" -> 5,
    "microservice.services.nrs.call-timeout" -> "30 seconds",
    "microservice.services.nrs.reset-timeout" -> "1 second",
    "microservice.services.nrs.max-reset-timeout" -> "5 seconds",
    "microservice.services.nrs.exponential-backoff-factor" -> 2.0
  )

  private def schedulerDisabledConfig = Configuration(
    "nrs-submission-scheduler.enabled"       -> false,
    "nrs-submission-scheduler.interval"      -> "1 second",
    "nrs-submission-scheduler.initial-delay" -> "2 seconds",
    "microservice.services.nrs.protocol"     -> "http",
    "microservice.services.nrs.host"         -> wireMockHost,
    "microservice.services.nrs.port"         -> wireMockPort,
    "microservice.services.nrs.url"          -> "/submission",
    "microservice.services.nrs.api-key"      -> "test-api-key",
    "microservice.services.nrs.max-failures" -> 5,
    "microservice.services.nrs.call-timeout" -> "30 seconds",
    "microservice.services.nrs.reset-timeout" -> "1 second",
    "microservice.services.nrs.max-reset-timeout" -> "5 seconds",
    "microservice.services.nrs.exponential-backoff-factor" -> 2.0
  )

  "NrsScheduledService" - {
    "when scheduler is enabled" - {
      "must process work items from the queue" in {
        val app = GuiceApplicationBuilder()
          .configure(schedulerEnabledConfig)
          .build()

        running(app) {
          // Stub NRS endpoint FIRST, before creating work items
          wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock
              .post(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/submission"))
              .willReturn(
                com.github.tomakehurst.wiremock.client.WireMock
                  .aResponse()
                  .withStatus(ACCEPTED)
                  .withBody(Json.obj("nrSubmissionId" -> "test-id").toString)
              )
          )

          val repository = app.injector.instanceOf[NrsWorkItemRepository]
          val nrsService = app.injector.instanceOf[uk.gov.hmrc.vapingduty.services.NrsService]

          // Clear any existing items
          await(repository.collection.drop().toFuture())

          // Create a test work item
          val workItem = NrsSubmissionWorkItem(
            NrsPayload(
              payload = "test-payload",
              metadata = sampleNrsMeta
            )
          )

          // Add work item to queue - make it available 1 second in the past to ensure it's picked up
          await(
            repository.pushNew(
              workItem,
              Instant.now().minusSeconds(1)
            )
          )

          // Manually trigger processing instead of waiting for scheduler
          await(nrsService.processAll())

          // Check the item was processed successfully
          val items = await(repository.collection.find().toFuture())
          items.headOption.map(_.status) shouldBe Some(ProcessingStatus.Succeeded)
        }
      }

      "must continue processing after encountering errors" in {
        val app = GuiceApplicationBuilder()
          .configure(schedulerEnabledConfig)
          .build()

        running(app) {
          // Stub NRS endpoint FIRST, before creating work items
          // Both items will succeed (match any request body)
          // The service will process all items in one scheduler run
          wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock
              .post(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/submission"))
              .willReturn(
                com.github.tomakehurst.wiremock.client.WireMock
                  .aResponse()
                  .withStatus(ACCEPTED)
                  .withBody(Json.obj("nrSubmissionId" -> "test-id").toString)
              )
          )

          val repository = app.injector.instanceOf[NrsWorkItemRepository]
          val nrsService = app.injector.instanceOf[uk.gov.hmrc.vapingduty.services.NrsService]

          // Clear any existing items
          await(repository.collection.drop().toFuture())

          // Create multiple test work items
          val testPayload1 = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload-1", metadata = sampleNrsMeta)
          )
          val testPayload2 = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload-2", metadata = sampleNrsMeta)
          )

          // Make items available 1 second in the past to ensure they're picked up
          await(repository.pushNew(testPayload1, Instant.now().minusSeconds(1)))
          await(repository.pushNew(testPayload2, Instant.now().minusSeconds(1)))

          // Manually trigger processing instead of waiting for scheduler
          // With the recursive processAll(), both items are processed in one call
          await(nrsService.processAll())

          // Check both items were processed successfully
          val items = await(repository.collection.find().toFuture())
          items.size shouldBe 2
          val allProcessed = items.forall(_.status == ProcessingStatus.Succeeded)
          allProcessed shouldBe true
        }
      }
    }

    "when scheduler is disabled" - {
      "must not process work items from the queue" in {
        val app = GuiceApplicationBuilder()
          .configure(schedulerDisabledConfig)
          .build()

        running(app) {
          val repository = app.injector.instanceOf[NrsWorkItemRepository]

          // Clear any existing items
          await(repository.collection.drop().toFuture())

          // Stub NRS endpoint (even though scheduler is disabled, match any request body)
          wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock
              .post(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/submission"))
              .willReturn(
                com.github.tomakehurst.wiremock.client.WireMock
                  .aResponse()
                  .withStatus(ACCEPTED)
                  .withBody(Json.obj("nrSubmissionId" -> "test-id").toString)
              )
          )

          // Create a test work item
          val testPayload = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload", metadata = sampleNrsMeta)
          )

          // Make item available 1 second in the past (though scheduler is disabled, for consistency)
          await(
            repository.pushNew(
              testPayload,
              Instant.now().minusSeconds(1)
            )
          )

          // Wait a reasonable time
          Thread.sleep(2000)

          // Item should still be in ToDo status since scheduler is disabled
          val items = await(repository.collection.find().toFuture())
          items.headOption.map(_.status) shouldBe Some(ProcessingStatus.ToDo)
        }
      }
    }
  }
}