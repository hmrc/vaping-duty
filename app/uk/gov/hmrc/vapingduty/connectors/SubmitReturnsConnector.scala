/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.connectors.helpers.HIPAuth
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.returns.submit.{ReturnCreateRequest, ReturnCreateResponse, ReturnSubmittedResponse}
import uk.gov.hmrc.vapingduty.utils.{DateTimeHelper, RandomUUIDGenerator}

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubmitReturnsConnector @Inject()(randomUUIDGenerator: RandomUUIDGenerator, clock: Clock)(
  config: AppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
  extends HttpReadsInstances
    with Logging {

  private val parsingError = "Parsing failed for VPD return submission response"

  def submitReturn(returnRequest: ReturnCreateRequest, vpdId: VpdId)
                  (implicit hc: HeaderCarrier): Future[ReturnSubmittedResponse] =
    httpClient
      .post(url"${config.submitReturnUrl()}")
      .setHeader(createReturnHeaders(vpdId, returnRequest.periodKey): _*)
      .withBody(Json.toJson(returnRequest))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recoverWith { case e: Exception =>
        logger.warn(s"Exception while submitting VPD return: ${e.getMessage}")
        Future.failed(InternalServerException("Failed to submit VPD return"))
      }
      .flatMap(response => submitReturnParser(response))

  private def submitReturnParser(response: Either[UpstreamErrorResponse, HttpResponse]): Future[ReturnSubmittedResponse] = {
    response match {
      case Right(response) =>
        Try {
          response.json.as[ReturnCreateResponse]
        } match {
          case Success(createResponse) =>
            Future.successful(createResponse.success)
          case Failure(_) =>
            logger.warn(parsingError)
            Future.failed(InternalServerException(parsingError))
        }
      case Left(error) =>
        logger.warn(s"Unexpected response from VPD return submission API. Status: ${error.statusCode} Message: ${error.message}")
        Future.failed(InternalServerException("Failed to submit VPD return"))
    }
  }

  def createReturnHeaders(vpdId: VpdId, periodKey: String): Seq[(String, String)] =
    Seq(
      (HeaderNames.authorisation, HIPAuth(config).authorizationForReturns()),
      ("correlationid", randomUUIDGenerator.uuid),
      ("X-Message-Type", "VPDReturnCreate"),
      ("X-Originating-System", "MDTP"),
      ("X-Receipt-Date", DateTimeHelper.formatISOInstantSeconds(Instant.now(clock))),
      ("X-Regime-Type", "VPD"),
      ("X-Transmitting-System", "HIP"),
      ("X-ZVPD", vpdId.toString)
    )
}
