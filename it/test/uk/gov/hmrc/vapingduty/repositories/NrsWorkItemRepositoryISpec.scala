/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingduty.repositories

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.vapingduty.base.ISpecBase
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsMetadata, NrsPayload, NrsSubmissionWorkItem}

import java.time.Instant
import scala.concurrent.Future

class NrsWorkItemRepositoryISpec extends ISpecBase with BeforeAndAfterEach {

  private val repository = app.injector.instanceOf[NrsWorkItemRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.collection.deleteMany(Filters.empty()).toFuture())
  }

  private val testNrsPayload = NrsPayload(
    payload = "encodedPayload",
    metadata = NrsMetadata(
      businessId = "vpd",
      notableEvent = "vaping-duty-return-submitted",
      payloadContentType = "application/json",
      payloadSha256Checksum = "checksum123",
      userSubmissionTimestamp = Instant.now(clock).toString,
      identityData = IdentityData(
        internalId = Some("Int-123"),
        externalId = Some("Ext-123"),
        agentCode = None,
        optionalCredentials = None,
        confidenceLevel = ConfidenceLevel.L200,
        nino = None,
        saUtr = None,
        optionalName = None,
        dateOfBirth = None,
        email = None,
        groupIdentifier = None,
        credentialRole = None,
        mdtpInformation = None,
        optionalItmpName = None,
        dateOfBirthFromItmp = None,
        optionalItmpAddress = None,
        affinityGroup = Some(AffinityGroup.Organisation),
        credentialStrength = Some("strong"),
        loginTimes = None
      ),
      userAuthToken = "Bearer token123",
      headerData = Map.empty[String, String],
      searchKeys = Map("vpdReference" -> "XMVPD0000000123")
    )
  )

  private val testWorkItem = NrsSubmissionWorkItem(testNrsPayload)

  "NrsWorkItemRepository must" - {
    "successfully push a new work item" in {
      val result = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      result.item mustBe testWorkItem
      result.status mustBe ProcessingStatus.ToDo
    }

    "pull an outstanding work item" in {
      await(repository.pushNew(testWorkItem, Instant.now(clock).minusSeconds(120), _ => ProcessingStatus.ToDo))

      val result = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))

      result.isDefined mustBe true
      result.get.item mustBe testWorkItem
      result.get.status mustBe ProcessingStatus.InProgress
    }

    "return None when no outstanding work items exist" in {
      val result = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))

      result.isDefined mustBe false
    }

    "mark a work item as complete" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      await(repository.complete(workItem.id, ProcessingStatus.Succeeded))

      val retrieved = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))
      retrieved mustBe None
    }

    "mark a work item as failed" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      val result = await(repository.markAs(workItem.id, ProcessingStatus.Failed, Some(Instant.now(clock).plusSeconds(60))))

      result mustBe true
    }

    "complete and delete a work item" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      await(repository.completeAndDelete(workItem.id))

      val result = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))
      result.isDefined mustBe false
    }

    "handle multiple work items" in {
      val workItem1 = testWorkItem
      val workItem2 = testWorkItem.copy(payload = testNrsPayload.copy(payload = "encodedPayload2"))

      await(repository.pushNew(workItem1, Instant.now(clock).minusSeconds(120), _ => ProcessingStatus.ToDo))
      await(repository.pushNew(workItem2, Instant.now(clock).minusSeconds(120), _ => ProcessingStatus.ToDo))

      val result1 = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))
      val result2 = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))

      result1.isDefined mustBe true
      result2.isDefined mustBe true
      result1.get.item must not be result2.get.item
    }
  }

  private def await[T](future: Future[T]): T = {
    scala.concurrent.Await.result(future, scala.concurrent.duration.Duration.Inf)
  }
}