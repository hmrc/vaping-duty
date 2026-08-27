package uk.gov.hmrc.vapingduty.controllers.actions

import com.google.inject.Inject
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.vapingduty.models.identifiers.InternalId
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with BackendHeaderCarrierProvider
    with AuthorisedFunctions {

  private val retrievals =
    Retrievals.internalId and
      Retrievals.externalId and
      Retrievals.agentCode and
      Retrievals.credentials and
      Retrievals.confidenceLevel and
      Retrievals.nino and
      Retrievals.saUtr and
      Retrievals.name and
      Retrievals.dateOfBirth and
      Retrievals.email and
      Retrievals.agentInformation and
      Retrievals.groupIdentifier and
      Retrievals.credentialRole and
      Retrievals.mdtpInformation and
      Retrievals.itmpName and
      Retrievals.itmpDateOfBirth and
      Retrievals.itmpAddress and
      Retrievals.affinityGroup and
      Retrievals.credentialStrength and
      Retrievals.loginTimes and
      Retrievals.allEnrolments

  override def invokeBlock[A](
    request: Request[A],
    block: IdentifierRequest[A] => Future[Result]
  ): Future[Result] = {

    implicit val req: Request[A] = request

    authorised()
      .retrieve(retrievals) {
        case internalId ~ externalId ~ agentCode ~ credentials ~ confidenceLevel ~ nino ~ saUtr ~ name ~ dateOfBirth ~ email ~ agentInformation ~ groupIdentifier ~ credentialRole ~ mdtpInformation ~ itmpName ~ itmpDateOfBirth ~ itmpAddress ~ affinityGroup ~ credentialStrength ~ loginTimes ~ enrolments =>
          internalId match {
            case Some(id) =>
              block(
                IdentifierRequest(
                  request = request,
                  internalId = InternalId(id),
                  externalId = externalId,
                  agentCode = agentCode,
                  credentials = credentials,
                  confidenceLevel = confidenceLevel,
                  nino = nino,
                  saUtr = saUtr,
                  name = name,
                  dateOfBirth = dateOfBirth,
                  email = email,
                  agentInformation = agentInformation,
                  groupIdentifier = groupIdentifier,
                  credentialRole = credentialRole,
                  mdtpInformation = mdtpInformation,
                  itmpName = itmpName,
                  itmpDateOfBirth = itmpDateOfBirth,
                  itmpAddress = itmpAddress,
                  affinityGroup = affinityGroup,
                  credentialStrength = credentialStrength,
                  loginTimes = loginTimes,
                  enrolments = enrolments
                )
              )
            case None =>
              Future.successful(Unauthorized)
          }
      }
      .recover { case _: AuthorisationException =>
        Unauthorized
      }
  }
}