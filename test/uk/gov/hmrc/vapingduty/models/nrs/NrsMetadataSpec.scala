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

package uk.gov.hmrc.vapingduty.models.nrs

import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ItmpAddress, ItmpName, MdtpInformation, Name}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}
import uk.gov.hmrc.vapingduty.base.SpecBase

class NrsMetadataSpec extends SpecBase {

  private val testPayload = """{"test":"payload"}"""
  private val testSha256Hash = "a7c976db1723adb41274178dc82e9b777941ab201c69de61d0f2bc6d27a32534"
  private val testTimestamp = "2024-01-15T10:30:00Z"
  private val testAuthToken = "Bearer test-token"
  private val testVpdId = "XMVPD0000000123"

  private val testIdentityData = IdentityData(
    internalId = Some("int-id-123"),
    externalId = Some("ext-id-456"),
    agentCode = None,
    optionalCredentials = Some(Credentials("cred-id", "GovernmentGateway")),
    confidenceLevel = ConfidenceLevel.L200,
    nino = Some("AB123456C"),
    saUtr = None,
    optionalName = Some(Name(Some("John"), Some("Doe"))),
    email = Some("test@example.com"),
    agentInformation = AgentInformation(None, None, None),
    groupIdentifier = Some("group-123"),
    credentialRole = Some(CredentialRole.User),
    mdtpInformation = Some(MdtpInformation("device-id", "session-id")),
    optionalItmpName = Some(ItmpName(Some("John"), None, Some("Doe"))),
    optionalItmpAddress = Some(ItmpAddress(
      Some("Line 1"),
      None,
      None,
      None,
      None,
      Some("AB12 3CD"),
      Some("GB"),
      Some("UK")
    )),
    affinityGroup = Some(AffinityGroup.Individual),
    credentialStrength = Some("strong")
  )

  private val testHeaderData = Map(
    "X-Request-ID" -> "test-request-id",
    "X-Session-ID" -> "test-session-id"
  )

  "NrsMetadata" - {
    "create" - {
      "must create NrsMetadata with correct values" in {
        val result = NrsMetadata.create(
          payLoad = testPayload,
          sha256Hash = testSha256Hash,
          identityData = testIdentityData,
          submissionTimeStamp = testTimestamp,
          userAuthToken = testAuthToken,
          userHeaderData = testHeaderData,
          vpdId = testVpdId
        )

        result.businessId mustBe "vpd"
        result.notableEvent mustBe "vpd-submit-return-api"
        result.payloadContentType mustBe "application/json"
        result.payloadSha256Checksum mustBe testSha256Hash
        result.userSubmissionTimestamp mustBe testTimestamp
        result.identityData mustBe testIdentityData
        result.userAuthToken mustBe testAuthToken
        result.headerData mustBe testHeaderData
        result.searchKeys mustBe Map("vpdId" -> testVpdId)
      }

      "must create NrsMetadata with empty header data" in {
        val result = NrsMetadata.create(
          payLoad = testPayload,
          sha256Hash = testSha256Hash,
          identityData = testIdentityData,
          submissionTimeStamp = testTimestamp,
          userAuthToken = testAuthToken,
          userHeaderData = Map.empty,
          vpdId = testVpdId
        )

        result.headerData mustBe Map.empty
      }
    }

    "must serialize to JSON correctly" in {
      val metadata = NrsMetadata.create(
        payLoad = testPayload,
        sha256Hash = testSha256Hash,
        identityData = testIdentityData,
        submissionTimeStamp = testTimestamp,
        userAuthToken = testAuthToken,
        userHeaderData = testHeaderData,
        vpdId = testVpdId
      )

      val json = Json.toJson(metadata)

      (json \ "businessId").as[String] mustBe "vpd"
      (json \ "notableEvent").as[String] mustBe "vpd-submit-return-api"
      (json \ "payloadContentType").as[String] mustBe "application/json"
      (json \ "payloadSha256Checksum").as[String] mustBe testSha256Hash
      (json \ "userSubmissionTimestamp").as[String] mustBe testTimestamp
      (json \ "userAuthToken").as[String] mustBe testAuthToken
      (json \ "searchKeys" \ "vpdId").as[String] mustBe testVpdId
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "businessId" -> "vpd",
        "notableEvent" -> "vpd-submit-return-api",
        "payloadContentType" -> "application/json",
        "payloadSha256Checksum" -> testSha256Hash,
        "userSubmissionTimestamp" -> testTimestamp,
        "identityData" -> Json.obj(
          "confidenceLevel" -> 200,
          "agentInformation" -> Json.obj()
        ),
        "userAuthToken" -> testAuthToken,
        "headerData" -> Json.obj(
          "X-Request-ID" -> "test-request-id"
        ),
        "searchKeys" -> Json.obj(
          "vpdId" -> testVpdId
        )
      )

      val result = json.as[NrsMetadata]

      result.businessId mustBe "vpd"
      result.notableEvent mustBe "vpd-submit-return-api"
      result.payloadSha256Checksum mustBe testSha256Hash
      result.searchKeys("vpdId") mustBe testVpdId
    }

    "must round-trip through JSON" in {
      val metadata = NrsMetadata.create(
        payLoad = testPayload,
        sha256Hash = testSha256Hash,
        identityData = testIdentityData,
        submissionTimeStamp = testTimestamp,
        userAuthToken = testAuthToken,
        userHeaderData = testHeaderData,
        vpdId = testVpdId
      )

      val json = Json.toJson(metadata)
      val result = json.as[NrsMetadata]

      result mustBe metadata
    }
  }
}