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

package uk.gov.hmrc.vapingduty.services

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, CredentialStrength, User}
import uk.gov.hmrc.http.{InternalServerException, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.NrsConnector
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsMetadata, NrsPayload, NrsSubmissionWorkItem}
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest
import uk.gov.hmrc.vapingduty.repositories.NrsWorkItemRepository
import uk.gov.hmrc.vapingduty.services.NrsService.NonRepudiationIdentityRetrievals
import uk.gov.hmrc.vapingduty.utils.{DateTimeService, NrsUtils}

import java.time.Instant
import scala.concurrent.Future

class NrsServiceSpec extends SpecBase {

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockNrsConnector: NrsConnector = mock[NrsConnector]
  val mockNrsUtils: NrsUtils = mock[NrsUtils]
  val mockDateTimeService: DateTimeService = mock[DateTimeService]

  val mockNrsWorkItemRepository: NrsWorkItemRepository = mock[NrsWorkItemRepository]

  val service = new NrsService(
    mockAuthConnector,
    clock,
    mockNrsConnector,
    mockNrsUtils,
    mockDateTimeService,
    mockNrsWorkItemRepository
  )

  val testPayload: JsValue = Json.obj("test" -> "data")
  val testIdentityData = IdentityData(
    internalId = Some("Int-123"),
    optionalCredentials = None,
    confidenceLevel = ConfidenceLevel.L200,
    groupIdentifier = Some("Group-123"),
    credentialRole = Some(User),
    affinityGroup = Some(Organisation),
    credentialStrength = Some("strong")
  )
  val testNotableEvent = "vaping-duty-return-submitted"
  val testEncodedPayload = "encodedPayload"
  val testChecksum = "checksum123"
  val testTimestamp: Instant = Instant.now(clock)
  val testTimestampString = "2024-06-11T12:34:27.838Z"
  val testHeaderData: JsObject = Json.obj("test" -> "header")
  val testSearchKeys: JsObject = Json.obj("vpdReference" -> "XMVPD0000000123")

  val testMetadata = NrsMetadata.create(
    payLoad = Json.stringify(testPayload),
    sha256Hash = testChecksum,
    identityData = testIdentityData,
    submissionTimeStamp = testTimestampString,
    userAuthToken = "Bearer token",
    userHeaderData = Map("User-Agent" -> "test-agent"),
    vpdId = "XMVPD0000000123",
    periodKey = periodKey.value
  )

  val testNrsPayload = NrsPayload(testEncodedPayload, testMetadata)
  val testNrsSubmissionWorkItem = NrsSubmissionWorkItem(testNrsPayload)

  val testWorkItem: WorkItem[NrsSubmissionWorkItem] = 
    WorkItem(
      id = new ObjectId(),
      receivedAt = Instant.now(clock),
      updatedAt = Instant.now(clock),
      availableAt = Instant.now(clock),
      status = ProcessingStatus.ToDo,
      failureCount = 0,
      item = testNrsSubmissionWorkItem
    )

  implicit class RetrievalCombiner[A](a: A) {
    def ~[B](b: B): A ~ B = new~(a, b)
  }
    
  val authRetrievals: NonRepudiationIdentityRetrievals =
    Some(Organisation) ~
      Some(internalId.id) ~
      Some("Group-123") ~
      Some(Credentials("testProviderId", "testProviderType")) ~
      ConfidenceLevel.L50 ~
      Some(User) ~
      Some(CredentialStrength.strong)

  "NrsService must" - {
    "makeWorkItemAndQueue must" - {
      "successfully queue a work item" in {
        reset(mockNrsWorkItemRepository, mockNrsUtils, mockDateTimeService, mockAuthConnector)

        when(mockAuthConnector.authorise(
          any(),
          eqTo(
            Retrievals.affinityGroup and
              Retrievals.internalId and
              Retrievals.groupIdentifier and
              Retrievals.credentials and
              Retrievals.confidenceLevel and
              Retrievals.credentialRole and
              Retrievals.credentialStrength
          )
        )(any(), any())).thenReturn(Future.successful(authRetrievals))

        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp).thenReturn(testTimestampString)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Unexpected null response")))

        implicit val request: IdentifierRequest[_] = IdentifierRequest(fakeRequest, "Int-123", "XMVPD0000000123")
        val result = service.makeWorkItemAndQueue(testPayload, testNotableEvent, periodKey.value)(using hc, request)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), any())
        }
      }

      "handle queueing failures gracefully" in {
        reset(mockNrsWorkItemRepository, mockNrsUtils, mockDateTimeService, mockAuthConnector)

        when(mockAuthConnector.authorise(
          any(),
          eqTo(
            Retrievals.affinityGroup and
              Retrievals.internalId and
              Retrievals.groupIdentifier and
              Retrievals.credentials and
              Retrievals.confidenceLevel and
              Retrievals.credentialRole and
              Retrievals.credentialStrength
          )
        )(any(), any())).thenReturn(Future.successful(authRetrievals))

        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp).thenReturn(testTimestampString)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Database error")))

        implicit val request: IdentifierRequest[_] = IdentifierRequest(fakeRequest, "Int-123", "XMVPD0000000123")
        val result = service.makeWorkItemAndQueue(testPayload, testNotableEvent, periodKey.value)(using hc, request)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), any())
        }
      }
    }

    "submitToNrs must" - {
      "successfully submit to NRS and return Right(())" in {
        reset(mockNrsConnector)

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Right(())))

        whenReady(service.submitToNrs(testNrsPayload)) { result =>
          result mustBe Right(())
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
        }
      }

      "return Left(UpstreamErrorResponse) when NRS connector fails" in {
        reset(mockNrsConnector)

        val error = UpstreamErrorResponse("NRS error", 500)

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Left(error)))

        whenReady(service.submitToNrs(testNrsPayload)) { result =>
          result mustBe Left(error)
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
        }
      }
    }

    "processAll must" - {
      "return successfully when no work items are available" in {
        reset(mockNrsWorkItemRepository)

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(None))

        whenReady(service.processAll()) { result =>
          result mustBe ()
          verify(mockNrsWorkItemRepository).pullOutstanding(any(), any())
        }
      }

      "successfully process a single work item and mark as succeeded" in {
        reset(mockNrsWorkItemRepository, mockNrsConnector)

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(testWorkItem)))
          .thenReturn(Future.successful(None))

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Right(())))

        when(mockNrsWorkItemRepository.complete(any(), any()))
          .thenReturn(Future.successful(true))

        whenReady(service.processAll()) { result =>
          result mustBe ()
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
          verify(mockNrsWorkItemRepository).complete(any(), any())
        }
      }

      "mark work item as failed when NRS submission fails" in {
        reset(mockNrsWorkItemRepository, mockNrsConnector)

        val error = UpstreamErrorResponse("NRS error", 500)

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(testWorkItem)))
          .thenReturn(Future.successful(None))

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Left(error)))

        when(mockNrsWorkItemRepository.markAs(any(), any(), any()))
          .thenReturn(Future.successful(true))

        whenReady(service.processAll()) { result =>
          result mustBe (())
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
          verify(mockNrsWorkItemRepository).markAs(any(), any(), any())
        }
      }

      "mark work item as failed when an exception occurs during processing" in {
        reset(mockNrsWorkItemRepository, mockNrsConnector)

        val exception = new RuntimeException("Processing error")

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(testWorkItem)))
          .thenReturn(Future.successful(None))

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.failed(exception))

        when(mockNrsWorkItemRepository.markAs(any(), any(), any()))
          .thenReturn(Future.successful(true))

        whenReady(service.processAll()) { result =>
          result mustBe (())
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
          verify(mockNrsWorkItemRepository).markAs(any(), any(), any())
        }
      }

      "process multiple work items in sequence" in {
        reset(mockNrsWorkItemRepository, mockNrsConnector)

        val workItem1 = testWorkItem
        val workItem2 = testWorkItem

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(workItem1)))
          .thenReturn(Future.successful(Some(workItem2)))
          .thenReturn(Future.successful(None))

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Right(())))

        when(mockNrsWorkItemRepository.complete(any(), any()))
          .thenReturn(Future.successful(true))

        whenReady(service.processAll()) { result =>
          result mustBe (())
          verify(mockNrsConnector, times(2)).submitToNrs(any[NrsPayload])(any())
          verify(mockNrsWorkItemRepository, times(2)).complete(any(), any())
        }
      }

      "continue processing after a failed work item" in {
        reset(mockNrsWorkItemRepository, mockNrsConnector)

        val workItem1 = testWorkItem
        val workItem2 = testWorkItem
        val error = UpstreamErrorResponse("NRS error", 500)

        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(workItem1)))
          .thenReturn(Future.successful(Some(workItem2)))
          .thenReturn(Future.successful(None))

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Left(error)))
          .thenReturn(Future.successful(Right(())))

        when(mockNrsWorkItemRepository.markAs(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(mockNrsWorkItemRepository.complete(any(), any()))
          .thenReturn(Future.successful(true))

        whenReady(service.processAll()) { result =>
          result mustBe ()
          verify(mockNrsConnector, times(2)).submitToNrs(any[NrsPayload])(any())
          verify(mockNrsWorkItemRepository).markAs(any(), any(), any())
          verify(mockNrsWorkItemRepository).complete(any(), any())
        }
      }
    }

    "makeWorkItemAndQueue must" - {
      "throw InternalServerException when no auth token is available" in {
        reset(mockNrsWorkItemRepository, mockNrsUtils, mockDateTimeService, mockAuthConnector)

        when(mockAuthConnector.authorise(
          any(),
          eqTo(
            Retrievals.affinityGroup and
              Retrievals.internalId and
              Retrievals.groupIdentifier and
              Retrievals.credentials and
              Retrievals.confidenceLevel and
              Retrievals.credentialRole and
              Retrievals.credentialStrength
          )
        )(any(), any())).thenReturn(Future.successful(authRetrievals))

        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp).thenReturn(testTimestampString)

        val hcWithoutAuth = hc.copy(authorization = None)
        implicit val request: IdentifierRequest[_] = IdentifierRequest(fakeRequest, "Int-123", "XMVPD0000000123")

        val exception = intercept[InternalServerException] {
          whenReady(service.makeWorkItemAndQueue(testPayload, testNotableEvent, periodKey.value)(using hcWithoutAuth, request)) { _ => }
        }

        exception.getMessage mustBe "No auth token available for NRS"
      }
    }
  }
}
