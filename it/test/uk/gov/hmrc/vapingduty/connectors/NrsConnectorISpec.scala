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

package uk.gov.hmrc.vapingduty.connectors

import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.vapingduty.base.ISpecBase
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsMetadata, NrsPayload}
import uk.gov.hmrc.vapingduty.utils.ConnectorTestHelpers

import java.time.Instant

class NrsConnectorISpec extends ISpecBase with ConnectorTestHelpers {

  protected val endpointName = "nrs"

  "NrsConnector must" - {
    "return Right(()) when NRS accepts the submission" in new SetUp {
      val nrsPayload = NrsPayload(
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
            confidenceLevel = ConfidenceLevel.L50,
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
            affinityGroup = Some(Organisation),
            credentialStrength = Some("strong"),
            loginTimes = None
          ),
          userAuthToken = "Bearer token123",
          headerData = Map.empty[String, String],
          searchKeys = Map("vpdReference" -> "XMVPD0000000123")
        )
      )

      stubPost(url, ACCEPTED, Json.toJson(nrsPayload).toString, "")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result mustBe Right(())
        verifyPost(url)
      }
    }

    "return Left(UpstreamErrorResponse) when NRS returns BAD_REQUEST" in new SetUp {
      val nrsPayload = NrsPayload(
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
            affinityGroup = Some(Organisation),
            credentialStrength = Some("strong"),
            loginTimes = None
          ),
          userAuthToken = "Bearer token123",
          headerData = Map.empty[String, String],
          searchKeys = Map("vpdReference" -> "XMVPD0000000123")
        )
      )

      stubPost(url, BAD_REQUEST, Json.toJson(nrsPayload).toString, "Bad request")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result.isLeft mustBe true
        result.left.map(_.statusCode mustBe BAD_REQUEST)
        verifyPost(url)
      }
    }

    "return Left(UpstreamErrorResponse) when NRS returns INTERNAL_SERVER_ERROR" in new SetUp {
      val nrsPayload = NrsPayload(
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
            affinityGroup = Some(Organisation),
            credentialStrength = Some("strong"),
            loginTimes = None
          ),
          userAuthToken = "Bearer token123",
          headerData = Map.empty[String, String],
          searchKeys = Map("vpdReference" -> "XMVPD0000000123")
        )
      )

      stubPost(url, INTERNAL_SERVER_ERROR, Json.toJson(nrsPayload).toString, "Internal server error")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result.isLeft mustBe true
        result.left.map(_.statusCode mustBe INTERNAL_SERVER_ERROR)
        verifyPost(url)
      }
    }

    "return Left(UpstreamErrorResponse) when NRS returns an unexpected error" in new SetUp {
      val nrsPayload = NrsPayload(
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
            affinityGroup = Some(Organisation),
            credentialStrength = Some("strong"),
            loginTimes = None
          ),
          userAuthToken = "Bearer token123",
          headerData = Map.empty[String, String],
          searchKeys = Map("vpdReference" -> "XMVPD0000000123")
        )
      )

      stubPost(url, INTERNAL_SERVER_ERROR, Json.toJson(nrsPayload).toString, "Unexpected error")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result.isLeft mustBe true
        result.left.map(_.statusCode mustBe INTERNAL_SERVER_ERROR)
        verifyPost(url)
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val connector: NrsConnector = app.injector.instanceOf[NrsConnector]
    lazy val url  = s"${app.injector.instanceOf[AppConfig].nrsBaseUrl}/submission"
  }
}
