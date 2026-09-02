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

import org.apache.pekko.pattern.CircuitBreaker
import play.api.Logging
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.models.nrs.NrsPayload
import uk.gov.hmrc.vapingduty.utils.RandomUUIDGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object NrsConnector {
  case class NrsCircuitBreaker(breaker: CircuitBreaker)
}

@Singleton
class NrsConnector @Inject()(
                              httpClient: HttpClientV2,
                              appConfig: AppConfig,
                              nrsCircuitBreaker: NrsConnector.NrsCircuitBreaker,
                              uuidGenerator: RandomUUIDGenerator
                            )(implicit ec: ExecutionContext) extends Logging {

  private val CORRELATION_ID_HEADER = "X-Correlation-Id"

  private def enforceCorrelationId(hc: HeaderCarrier): HeaderCarrier = {
    hc.headers(Seq(CORRELATION_ID_HEADER)).headOption match {
      case Some(_) => hc
      case None =>
        val correlationId = uuidGenerator.uuid
        logger.info(s"Generated correlation ID for NRS submission: $correlationId")
        hc.withExtraHeaders(CORRELATION_ID_HEADER -> correlationId)
    }
  }

  private def shouldTripCircuitBreaker: Try[HttpResponse] => Boolean = {
    case Success(response) => is5xx(response.status)
    case Failure(_) => true
  }

  def submitToNrs(payload: NrsPayload)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    val hcWithCorrelationId = enforceCorrelationId(hc)
    val correlationId = hcWithCorrelationId
      .headers(Seq(CORRELATION_ID_HEADER))
      .headOption
      .map(_._2)
      .getOrElse("unknown")
    val vpdReference = payload.metadata.searchKeys.getOrElse("vpdReference", "No VPD reference")

    logger.info(s"NRS submission: CorrelationId: $correlationId, VPD Reference: $vpdReference")

    val httpCall = httpClient
      .post(url"${appConfig.nrsSubmissionUrl}")(hcWithCorrelationId)
      .setHeader("X-API-Key" -> appConfig.nrsApiKey)
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]

    nrsCircuitBreaker.breaker
      .withCircuitBreaker(httpCall, shouldTripCircuitBreaker)
      .map { response =>
        response.status match {
          case ACCEPTED =>
            logger.info(s"NRS submission successful for CorrelationId: $correlationId")
            Right(())
          case status =>
            logger.warn(s"NRS submission failed with status: $status for CorrelationId: $correlationId")
            Left(UpstreamErrorResponse(response.body, status))
        }
      }
      .recover { case e: Exception =>
        logger.error(s"NRS submission failed with error for CorrelationId: $correlationId: ${e.getMessage}", e)
        Left(UpstreamErrorResponse(e.getMessage, INTERNAL_SERVER_ERROR))
      }
  }
}
