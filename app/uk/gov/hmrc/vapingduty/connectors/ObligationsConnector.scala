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

import play.api.Logging
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.connectors.helpers.Headers
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.obligations.ObligationsResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ObligationsConnector @Inject()(
                                      config: AppConfig,
                                      headers: Headers,
                                      implicit val httpClient: HttpClientV2
                                    )(using ExecutionContext)
  extends HttpReadsInstances
    with Logging {

  def getObligations(vpdId: VpdId)(using HeaderCarrier): Future[ObligationsResponse] =
    httpClient
      .get(url"${config.getObligationsUrl(vpdId)}")
      .setHeader(headers.createObligationsHeaders: _*)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap(response => responseParser(response))
      .recoverWith { case _: Exception =>
        logger.warn("An exception was returned while trying to fetch obligations")
        Future.failed(InternalServerException("Failed to get obligations"))
      }

  private def responseParser(response: Either[UpstreamErrorResponse, HttpResponse]): Future[ObligationsResponse] = {
    response match {
      case Right(httpResponse) =>
        Try {
          httpResponse.json.as[ObligationsResponse]
        } match {
          case Success(obligations) =>
            Future.successful(obligations)
          case Failure(_) =>
            logger.warn("Unable to parse obligations response")
            Future.failed(InternalServerException("Failed to get obligations"))
        }
      case Left(error) =>
        logger.warn(s"Unexpected response from obligations API. Status: ${error.statusCode}")
        Future.failed(InternalServerException("Failed to get obligations"))
    }
  }
}
