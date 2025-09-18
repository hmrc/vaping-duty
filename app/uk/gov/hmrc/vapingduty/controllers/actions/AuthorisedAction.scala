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

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.CredentialStrength.strong
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{authorisedEnrolments, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisedAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with BackendHeaderCarrierProvider
    with ActionFunction[Request, IdentifierRequest]

class BaseAuthorisedAction @Inject() (
  override val authConnector: AuthConnector,
  config: AppConfig,
  val parser: BodyParser[AnyContent]
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedAction
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val headerCarrier: HeaderCarrier = hc(request)

    authorised(
      AuthProviders(GovernmentGateway)
        and Enrolment(config.enrolmentServiceName)
        and CredentialStrength(strong)
        and Organisation
        and ConfidenceLevel.L50
    ).retrieve(internalId and authorisedEnrolments) { case optInternalId ~ enrolments =>
      val internalId: String = getOrElseFailWithUnauthorised(optInternalId, "Unable to retrieve internalId")
      block(IdentifierRequest(request, getAppaId(enrolments), internalId))
    } recover { case e: AuthorisationException =>
      logger.debug("Got AuthorisationException:", e)
      Unauthorized(
        Json.toJson(
          ErrorResponse(
            UNAUTHORIZED,
            e.reason
          )
        )
      )
    }
  }

  private def getAppaId(enrolments: Enrolments): String = {
    val adrEnrolments: Enrolment = getOrElseFailWithUnauthorised(
      enrolments.enrolments.find(_.key == config.enrolmentServiceName),
      s"Unable to retrieve enrolment: ${config.enrolmentServiceName}"
    )

    val key = config.enrolmentIdentifierKey

    val vpaIdOpt: Option[String] =
      adrEnrolments.getIdentifier(key).map(_.value)
    getOrElseFailWithUnauthorised(vpaIdOpt, s"Unable to retrieve $key from enrolments")
  }

  private def getOrElseFailWithUnauthorised[T](o: Option[T], failureMessage: String): T =
    o.getOrElse {
      logger.warn(s"Authorised Action failed with error: $failureMessage")
      throw new IllegalStateException(failureMessage)
    }
}
