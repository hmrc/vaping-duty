/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.vapingduty.controllers.actions

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolments, User}
import uk.gov.hmrc.vapingduty.models.identifiers.InternalId
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest

import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeAuthorisedAction @Inject()(parser: BodyParsers.Default) 
    extends AuthorisedAction(mock[AuthConnector], parser)(ExecutionContext.global) 
    with MockitoSugar {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(
      IdentifierRequest(
        request = request,
        internalId = InternalId("test-internal-id"),
        externalId = Some("test-external-id"),
        agentCode = None,
        credentials = Some(Credentials("test-cred-id", "GovernmentGateway")),
        confidenceLevel = ConfidenceLevel.L50,
        nino = Some("AB123456C"),
        saUtr = None,
        name = Some(Name(Some("Test"), Some("User"))),
        dateOfBirth = Some(LocalDate.of(1980, 1, 1)),
        email = Some("test@example.com"),
        agentInformation = AgentInformation(None, None, None),
        groupIdentifier = Some("test-group-id"),
        credentialRole = Some(User),
        mdtpInformation = Some(MdtpInformation("test-device-id", "test-session-id")),
        itmpName = Some(ItmpName(Some("Test"), None, Some("User"))),
        itmpDateOfBirth = Some(LocalDate.of(1980, 1, 1)),
        itmpAddress = None,
        affinityGroup = Some(AffinityGroup.Individual),
        credentialStrength = Some("strong"),
        loginTimes = LoginTimes(Instant.now(), None),
        enrolments = Enrolments(Set.empty)
      )
    )

}
