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
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.returns.view.ReturnDisplayResponse
import uk.gov.hmrc.vapingduty.utils.{DateTimeHelper, RandomUUIDGenerator}

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class GetReturnsConnector @Inject()(randomUUIDGenerator: RandomUUIDGenerator, clock: Clock)(
                                      config: AppConfig,
                                      implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  private val parsingError = "Parsing failed for VPD return get response"

  def getReturn(periodKey: String, vpdId: VpdId)
                  (implicit hc: HeaderCarrier): Future[ReturnDisplayResponse] =
    httpClient
      .get(url"${config.getReturnUrl(vpdId, periodKey)}")
      .setHeader(createReturnHeaders(vpdId): _*)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recoverWith { case e: Exception =>
        logger.warn(s"Exception while getting return: ${e.getMessage}")
        Future.failed(InternalServerException("Failed to get return"))
      }
      .flatMap(getReturn)
      .flatMap(parseJson)

  private def getReturn(response: Either[UpstreamErrorResponse, HttpResponse]): Future[HttpResponse] = {
    response match {
      case Right(response) => Future.successful(response)
      case Left(error) =>
        logger.warn(s"Unexpected response from VPD return get API. Status: ${error.statusCode}")
        Future.failed(InternalServerException("Failed to get VPD return"))
    }
  }

  private def parseJson(response: HttpResponse) = {
    Try {
      response.json.as[ReturnDisplayResponse]
    } match {
      case Success(getResponse) =>
        Future.successful(getResponse)
      case Failure(_) =>
        logger.warn(parsingError)
        Future.failed(InternalServerException(parsingError))
    }
  }

  def createReturnHeaders(vpdId: VpdId): Seq[(String, String)] =
    Seq(
      ("correlationid", randomUUIDGenerator.uuid),
      ("X-Message-Type", "VPDReturnDisplay"),
      ("X-Originating-System", "MDTP"),
      ("X-Receipt-Date", DateTimeHelper.formatISOInstantSeconds(Instant.now(clock))),
      ("X-Regime-Type", "VPD"),
      ("X-Transmitting-System", "HIP"),
      ("X-ZVPD", vpdId.toString)
    )
}
