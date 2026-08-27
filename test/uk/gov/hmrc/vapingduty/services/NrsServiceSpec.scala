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
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsPayload}
import uk.gov.hmrc.vapingduty.utils.{DateTimeService, NrsUtils}

import java.time.Instant
import scala.concurrent.Future

class NrsServiceSpec extends SpecBase {

  val mockNrsConnector: NrsConnector       = mock[NrsConnector]
  val mockNrsUtils: NrsUtils               = mock[NrsUtils]
  val mockDateTimeService: DateTimeService = mock[DateTimeService]

  val service = new NrsService(mockNrsConnector, mockNrsUtils, mockDateTimeService)

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
    "successfully submit to NRS and return Right(())" in {
      when(mockNrsUtils.encode(eqTo(testPayload)))
        .thenReturn(testEncodedPayload)
      when(mockNrsUtils.sha256Hash(eqTo(testPayload.toString)))
        .thenReturn(testChecksum)
      when(mockDateTimeService.timestamp())
        .thenReturn(testTimestamp)
      when(mockNrsUtils.buildHeaderData(any()))
        .thenReturn(testHeaderData)
      when(mockNrsUtils.buildSearchKeys(eqTo(testIdentityData)))
        .thenReturn(testSearchKeys)
      when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
        .thenReturn(Future.successful(Right(())))

      whenReady(service.submitToNrs(testPayload, testIdentityData, testNotableEvent)) { result =>
        result mustBe Right(())
        verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
      }
    }

    "return Left(UpstreamErrorResponse) when NRS connector fails" in {
      val error = UpstreamErrorResponse("NRS error", 500)

      when(mockNrsUtils.encode(eqTo(testPayload)))
        .thenReturn(testEncodedPayload)
      when(mockNrsUtils.sha256Hash(eqTo(testPayload.toString)))
        .thenReturn(testChecksum)
      when(mockDateTimeService.timestamp())
        .thenReturn(testTimestamp)
      when(mockNrsUtils.buildHeaderData(any()))
        .thenReturn(testHeaderData)
      when(mockNrsUtils.buildSearchKeys(eqTo(testIdentityData)))
        .thenReturn(testSearchKeys)
      when(mockNrsConnector.submitToNrs(any[NrsPayload])(any()))
        .thenReturn(Future.successful(Left(error)))

      whenReady(service.submitToNrs(testPayload, testIdentityData, testNotableEvent)) { result =>
        result mustBe Left(error)
        verify(mockNrsConnector).submitToNrs(any[NrsPayload])(any())
      }
    }
  }
}
