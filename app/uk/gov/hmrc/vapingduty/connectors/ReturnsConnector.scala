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
import uk.gov.hmrc.vapingduty.connectors.helpers.ReturnsHeaders
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.returns.{ReturnCreateRequest, ReturnCreateResponse, ReturnSubmittedResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ReturnsConnector @Inject()(
                                      config: AppConfig,
                                      headers: ReturnsHeaders,
                                      implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def submitReturn(returnRequest: ReturnCreateRequest, vpdId: VpdId)
                  (implicit hc: HeaderCarrier): Future[ReturnSubmittedResponse] =
    httpClient
      .post(url"${config.submitReturnUrl()}")
      .setHeader(headers.createReturnHeaders(vpdId): _*)
      .withBody(Json.toJson(returnRequest))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap(response => submitReturnParser(response))
      .recoverWith { case _: Exception =>
        logger.warn("An exception was returned while trying to submit VPD return")
        Future.failed(InternalServerException("Failed to submit VPD return"))
      }

  private def submitReturnParser(response: Either[UpstreamErrorResponse, HttpResponse]): Future[ReturnSubmittedResponse] = {
    response match {
      case Right(response) =>
        Try {
          response.json.as[ReturnCreateResponse]
        } match {
          case Success(createResponse) =>
            Future.successful(createResponse.success)
          case Failure(_) =>
            logger.warn("Parsing failed for VPD return submission response")
            Future.failed(InternalServerException("Failed to submit VPD return"))
        }
      case Left(error) =>
        logger.warn(s"Unexpected response from VPD return submission API. Status: ${error.statusCode}")
        Future.failed(InternalServerException("Failed to submit VPD return"))
    }
  }
}