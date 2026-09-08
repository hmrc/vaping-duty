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
import uk.gov.hmrc.vapingduty.connectors.helpers.HIPAuth
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.obligations.ObligationsResponse
import uk.gov.hmrc.vapingduty.utils.{DateTimeHelper, RandomUUIDGenerator}

import java.time.{Clock, Instant, LocalDate, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ObligationsConnector @Inject()(
                                      config: AppConfig,
                                      randomUUIDGenerator: RandomUUIDGenerator,
                                      clock: Clock,
                                      implicit val httpClient: HttpClientV2
                                    )(using ExecutionContext)
  extends HttpReadsInstances
    with Logging {

  private val parsingError = "Unable to parse obligations response"
  private val UK_ZONE = "Europe/London"
  
  private def calculateDateRange(): (String, String) = {
    val ukZone = ZoneId.of(UK_ZONE)
    val today = LocalDate.now(clock.withZone(ukZone))
    val fromDate = today.minusYears(config.obligationsYearsToLookBack)
    
    (DateTimeHelper.formatLocalDate(fromDate), DateTimeHelper.formatLocalDate(today))
  }
  
  def getObligations(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[ObligationsResponse] = {
    val (fromDate, toDate) = calculateDateRange()
    
    httpClient
      .get(url"${config.getObligationsUrl(vpdId, fromDate, toDate)}")
      .setHeader(createObligationHeaders: _*)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recoverWith { case _: Exception =>
        logger.warn("An exception was returned while trying to fetch obligations")
        Future.failed(InternalServerException("Failed to get obligations"))
      }
      .flatMap(response => responseParser(response))
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
            logger.warn(parsingError)
            Future.failed(InternalServerException(parsingError))
        }
      case Left(error) =>
        logger.warn(s"Unexpected response from obligations API. Status: ${error.statusCode} Message: ${error.message}")
        Future.failed(InternalServerException("Failed to get obligations"))
    }
  }

  private def createObligationHeaders: Seq[(String, String)] =
    Seq(
      (HeaderNames.authorisation, HIPAuth(config).authorizationForObligations()),
      ("correlationid", randomUUIDGenerator.uuid),
      ("X-Message-Type", "GetObligations"),
      ("X-Originating-System", "MDTP"),
      ("X-Receipt-Date", DateTimeHelper.formatISOInstantSeconds(Instant.now(clock))),
      ("X-Regime", "VPD"),
      ("X-Transmitting-System", "HIP")
    )
}
