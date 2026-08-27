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

package uk.gov.hmrc.vapingduty.models.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, Enrolments}
import uk.gov.hmrc.vapingduty.models.identifiers.InternalId
import uk.gov.hmrc.vapingduty.models.nrs.IdentityData

import java.time.LocalDate

final case class IdentifierRequest[A](
  request: Request[A],
  internalId: InternalId,
  externalId: Option[String],
  agentCode: Option[String],
  credentials: Option[Credentials],
  confidenceLevel: ConfidenceLevel,
  nino: Option[String],
  saUtr: Option[String],
  name: Option[Name],
  dateOfBirth: Option[LocalDate],
  email: Option[String],
  agentInformation: AgentInformation,
  groupIdentifier: Option[String],
  credentialRole: Option[CredentialRole],
  mdtpInformation: Option[MdtpInformation],
  itmpName: Option[ItmpName],
  itmpDateOfBirth: Option[LocalDate],
  itmpAddress: Option[ItmpAddress],
  affinityGroup: Option[AffinityGroup],
  credentialStrength: Option[String],
  loginTimes: LoginTimes,
  enrolments: Enrolments
) extends WrappedRequest[A](request) {

  def toIdentityData: IdentityData = IdentityData(
    internalId = Some(internalId.id),
    externalId = externalId,
    agentCode = agentCode,
    optionalCredentials = credentials,
    confidenceLevel = confidenceLevel,
    nino = nino,
    saUtr = saUtr,
    optionalName = name,
    dateOfBirth = dateOfBirth.map(_.toString),
    email = email,
    agentInformation = agentInformation,
    groupIdentifier = groupIdentifier,
    credentialRole = credentialRole,
    mdtpInformation = mdtpInformation,
    optionalItmpName = itmpName,
    dateOfBirthFromItmp = itmpDateOfBirth.map(_.toString),
    optionalItmpAddress = itmpAddress,
    affinityGroup = affinityGroup,
    credentialStrength = credentialStrength,
    loginTimes = Some(loginTimes)
  )
}
