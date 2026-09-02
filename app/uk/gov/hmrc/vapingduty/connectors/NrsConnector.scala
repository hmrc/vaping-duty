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
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.models.nrs.NrsPayload

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object NrsConnector {
  case class NrsCircuitBreaker(breaker: CircuitBreaker)
}

@Singleton
class NrsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  nrsCircuitBreaker: NrsConnector.NrsCircuitBreaker
)(implicit ec: ExecutionContext)
    extends Logging {

  private val nrsSubmissionUrl = s"${appConfig.nrsBaseUrl}/submission"

  def submitToNrs(payload: NrsPayload)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    val url = url"$nrsSubmissionUrl"

    nrsCircuitBreaker.breaker.withCircuitBreaker(
      httpClient
        .post(url)
        .setHeader("X-API-Key" -> appConfig.nrsApiKey)
        .withBody(Json.toJson(payload))
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case ACCEPTED =>
              logger.info("NRS submission successful")
              Right(())
            case status =>
              logger.warn(s"NRS submission failed with status: $status")
              Left(UpstreamErrorResponse(response.body, status))
          }
        }
        .recover { case e: UpstreamErrorResponse =>
          logger.error(s"NRS submission failed with error: ${e.getMessage}", e)
          Left(e)
        }
    )
  }
}
