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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.NrsConnector
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsPayload}
import uk.gov.hmrc.vapingduty.repositories.NrsWorkItemRepository
import uk.gov.hmrc.vapingduty.utils.{DateTimeService, NrsUtils}

import java.time.Instant
import scala.concurrent.Future

class NrsServiceSpec extends SpecBase {

  val mockNrsConnector: NrsConnector       = mock[NrsConnector]
  val mockNrsUtils: NrsUtils               = mock[NrsUtils]
  val mockDateTimeService: DateTimeService = mock[DateTimeService]

  val mockNrsWorkItemRepository: NrsWorkItemRepository = mock[NrsWorkItemRepository]

  val service = new NrsService(
    mockNrsConnector,
    mockNrsUtils,
    mockDateTimeService,
    mockNrsWorkItemRepository
  )

  val testPayload: JsValue = Json.obj("test" -> "data")
  val testIdentityData     = IdentityData(
    internalId = Some("Int-123"),
    externalId = Some("Ext-123"),
    agentCode = None,
    credentials = None,
    confidenceLevel = 200,
    nino = None,
    saUtr = None,
    name = None,
    dateOfBirth = None,
    email = None,
    agentInformation = None,
    groupIdentifier = None,
    credentialRole = None,
    mdtpInformation = None,
    itmpName = None,
    itmpDateOfBirth = None,
    itmpAddress = None,
    affinityGroup = Some("Organisation"),
    credentialStrength = Some("strong"),
    loginTimes = None
  )
  val testNotableEvent     = "vaping-duty-return-submitted"
  val testEncodedPayload   = "encodedPayload"
  val testChecksum         = "checksum123"
  val testTimestamp        = Instant.now(clock)
  val testHeaderData       = Json.obj("test" -> "header")
  val testSearchKeys       = Json.obj("vpdReference" -> "XMVPD0000000123")

  "NrsService must" - {
    "makeWorkItemAndQueue must" - {
      "successfully queue a work item" in {
        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp()).thenReturn(testTimestamp)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any())(any()))
          .thenReturn(Future.successful(null))

        val result = service.makeWorkItemAndQueue(testPayload, testIdentityData, testNotableEvent)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), eqTo(ProcessingStatus.ToDo))(any())
        }
      }

      "handle queueing failures gracefully" in {
        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp()).thenReturn(testTimestamp)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Database error")))

        val result = service.makeWorkItemAndQueue(testPayload, testIdentityData, testNotableEvent)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), eqTo(ProcessingStatus.ToDo))(any())
        }
      }
    }

    "submitToNrs must" - {
      "successfully submit to NRS and return Right(())" in {
        val testNrsPayload = NrsPayload(testEncodedPayload, null)

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Right(())))

        whenReady(service.submitToNrs(testNrsPayload)) { result =>
          result mustBe Right(())
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
        }
      }

      "return Left(UpstreamErrorResponse) when NRS connector fails" in {
        val error = UpstreamErrorResponse("NRS error", 500)
        val testNrsPayload = NrsPayload(testEncodedPayload, null)

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Left(error)))

        whenReady(service.submitToNrs(testNrsPayload)) { result =>
          result mustBe Left(error)
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
        }
      }
    }
  }
}
    "makeWorkItemAndQueue must" - {
      "successfully queue a work item" in {
        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp()).thenReturn(testTimestamp)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any())(any()))
          .thenReturn(Future.successful(null))

        val result = service.makeWorkItemAndQueue(testPayload, testIdentityData, testNotableEvent)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), eqTo(ProcessingStatus.ToDo))(any())
        }
      }

      "handle queueing failures gracefully" in {
        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp()).thenReturn(testTimestamp)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Database error")))

        val result = service.makeWorkItemAndQueue(testPayload, testIdentityData, testNotableEvent)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), eqTo(ProcessingStatus.ToDo))(any())
        }
      }
    }

    "submitToNrs must" - {
      "successfully submit to NRS and return Right(())" in {
        val testNrsPayload = NrsPayload(testEncodedPayload, null)

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Right(())))

        whenReady(service.submitToNrs(testNrsPayload)) { result =>
          result mustBe Right(())
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
        }
      }

      "return Left(UpstreamErrorResponse) when NRS connector fails" in {
        val error = UpstreamErrorResponse("NRS error", 500)
        val testNrsPayload = NrsPayload(testEncodedPayload, null)

        when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
          .thenReturn(Future.successful(Left(error)))

        whenReady(service.submitToNrs(testNrsPayload)) { result =>
          result mustBe Left(error)
          verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
        }
      }
    }
  }
}
  }
}
