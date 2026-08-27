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
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}
import uk.gov.hmrc.vapingduty.base.SpecBase

class IdentityDataSpec extends SpecBase {

  private val testIdentityData = IdentityData(
    internalId = Some("int-id-123"),
    externalId = Some("ext-id-456"),
    agentCode = Some("AGENT001"),
    optionalCredentials = Some(Credentials("cred-id", "GovernmentGateway")),
    confidenceLevel = ConfidenceLevel.L200,
    nino = Some("AB123456C"),
    saUtr = Some("1234567890"),
    optionalName = Some(Name(Some("John"), Some("Doe"))),
    email = Some("test@example.com"),
    agentInformation = AgentInformation(Some("agent-id"), Some("agent-code"), Some("agent-name")),
    groupIdentifier = Some("group-123"),
    credentialRole = Some(CredentialRole.User),
    mdtpInformation = Some(MdtpInformation("device-id", "session-id")),
    optionalItmpName = Some(ItmpName(Some("John"), Some("M"), Some("Doe"))),
    optionalItmpAddress = Some(ItmpAddress(
      Some("Line 1"),
      Some("Line 2"),
      Some("Line 3"),
      Some("Line 4"),
      Some("Line 5"),
      Some("AB12 3CD"),
      Some("GB"),
      Some("UK")
    )),
    affinityGroup = Some(AffinityGroup.Individual),
    credentialStrength = Some("strong")
  )

  "IdentityData" - {
    "must serialize to JSON correctly" in {
      val json = Json.toJson(testIdentityData)
      
      (json \ "internalId").as[String] mustBe "int-id-123"
      (json \ "externalId").as[String] mustBe "ext-id-456"
      (json \ "agentCode").as[String] mustBe "AGENT001"
      (json \ "confidenceLevel").as[Int] mustBe 200
      (json \ "nino").as[String] mustBe "AB123456C"
      (json \ "email").as[String] mustBe "test@example.com"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "internalId" -> "int-id-123",
        "externalId" -> "ext-id-456",
        "agentCode" -> "AGENT001",
        "optionalCredentials" -> Json.obj(
          "providerId" -> "cred-id",
          "providerType" -> "GovernmentGateway"
        ),
        "confidenceLevel" -> 200,
        "nino" -> "AB123456C",
        "saUtr" -> "1234567890",
        "optionalName" -> Json.obj(
          "name" -> "John",
          "lastName" -> "Doe"
        ),
        "email" -> "test@example.com",
        "agentInformation" -> Json.obj(
          "agentId" -> "agent-id",
          "agentCode" -> "agent-code",
          "agentFriendlyName" -> "agent-name"
        ),
        "groupIdentifier" -> "group-123",
        "credentialRole" -> "User",
        "mdtpInformation" -> Json.obj(
          "deviceId" -> "device-id",
          "sessionId" -> "session-id"
        ),
        "optionalItmpName" -> Json.obj(
          "givenName" -> "John",
          "middleName" -> "M",
          "familyName" -> "Doe"
        ),
        "optionalItmpAddress" -> Json.obj(
          "line1" -> "Line 1",
          "line2" -> "Line 2",
          "line3" -> "Line 3",
          "line4" -> "Line 4",
          "line5" -> "Line 5",
          "postCode" -> "AB12 3CD",
          "countryName" -> "GB",
          "countryCode" -> "UK"
        ),
        "affinityGroup" -> "Individual",
        "credentialStrength" -> "strong"
      )

      val result = json.as[IdentityData]
      
      result.internalId mustBe Some("int-id-123")
      result.externalId mustBe Some("ext-id-456")
      result.nino mustBe Some("AB123456C")
      result.email mustBe Some("test@example.com")
    }

    "must handle minimal IdentityData" in {
      val minimalData = IdentityData(
        confidenceLevel = ConfidenceLevel.L50,
        agentInformation = AgentInformation(None, None, None)
      )

      val json = Json.toJson(minimalData)
      val result = json.as[IdentityData]
      
      result.internalId mustBe None
      result.externalId mustBe None
      result.nino mustBe None
      result.email mustBe None
      result.confidenceLevel mustBe ConfidenceLevel.L50
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(testIdentityData)
      val result = json.as[IdentityData]
      
      result mustBe testIdentityData
    }
  }
}