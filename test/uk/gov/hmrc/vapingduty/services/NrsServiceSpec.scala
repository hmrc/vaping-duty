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
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.AgentInformation
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.NrsConnector
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsMetadata, NrsPayload}
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
    optionalCredentials = None,
    confidenceLevel = ConfidenceLevel.L200,
    nino = None,
    saUtr = None,
    optionalName = None,
    dateOfBirth = None,
    email = None,
    agentInformation = AgentInformation(None, None, None),
    groupIdentifier = None,
    credentialRole = None,
    mdtpInformation = None,
    optionalItmpName = None,
    dateOfBirthFromItmp = None,
    optionalItmpAddress = None,
    affinityGroup = None,
    credentialStrength = Some("strong"),
    loginTimes = None
  )
  val testNotableEvent     = "vaping-duty-return-submitted"
  val testEncodedPayload   = "encodedPayload"
  val testChecksum         = "checksum123"
  val testTimestamp        = Instant.now(clock)
  val testTimestampString  = "2024-06-11T12:34:27.838Z"
  val testHeaderData       = Json.obj("test" -> "header")
  val testSearchKeys       = Json.obj("vpdReference" -> "XMVPD0000000123")
  
  val testMetadata = NrsMetadata.create(
    payLoad = Json.stringify(testPayload),
    sha256Hash = testChecksum,
    identityData = testIdentityData,
    submissionTimeStamp = testTimestampString,
    userAuthToken = "Bearer token",
    userHeaderData = Map("User-Agent" -> "test-agent"),
    vpdId = "Int-123"
  )

  "NrsService must" - {
    "makeWorkItemAndQueue must" - {
      "successfully queue a work item" in {
        reset(mockNrsWorkItemRepository, mockNrsUtils, mockDateTimeService)
        
        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp).thenReturn(testTimestampString)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any()))
          .thenReturn(Future.successful(null))

        val result = service.makeWorkItemAndQueue(testPayload, testIdentityData, testNotableEvent)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), any())
        }
      }

      "handle queueing failures gracefully" in {
        reset(mockNrsWorkItemRepository, mockNrsUtils, mockDateTimeService)
        
        when(mockNrsUtils.encode(any[String])).thenReturn(testEncodedPayload)
        when(mockNrsUtils.sha256Hash(any[String])).thenReturn(testChecksum)
        when(mockDateTimeService.timestamp).thenReturn(testTimestampString)
        when(mockNrsWorkItemRepository.pushNew(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Database error")))

        val result = service.makeWorkItemAndQueue(testPayload, testIdentityData, testNotableEvent)

        whenReady(result) { _ =>
          verify(mockNrsWorkItemRepository).pushNew(any(), any(), any())
        }
      }
    }

    "submitToNrs must" - {
      "successfully submit to NRS and return Right(())" in {
        reset(mockNrsConnector)
        
        val testNrsPayload = NrsPayload(testEncodedPayload, testMetadata)

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
        val testNrsPayload = NrsPayload(testEncodedPayload, testMetadata)

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
