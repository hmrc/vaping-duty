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

package uk.gov.hmrc.vapingduty.services

import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.connectors.SubmitReturnsConnector
import uk.gov.hmrc.vapingduty.models.identifiers.{PeriodKey, VpdId}
import uk.gov.hmrc.vapingduty.models.nrs.NrsMetadata
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest
import uk.gov.hmrc.vapingduty.models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsSubmissionService @Inject()(
                                          submitReturnsConnector: SubmitReturnsConnector,
                                          nrsService: NrsService,
                                          appConfig: AppConfig
                                        )(implicit ec: ExecutionContext) extends Logging {

  private val NOTABLE_EVENT_SUBMIT_RETURN = NrsMetadata.notableEventSubmitReturn

  /**
   * Submit a return to ETMP and queue NRS work item for background processing.
   *
   * @param returnCreateRequest The return data to submit
   * @param vpdId               The VPD identifier
   * @param periodKey           The period key for the return
   * @param hc                  HeaderCarrier for HTTP context
   * @param request             The identifier request containing auth data
   * @return Future[ReturnSubmittedResponse] The submission response from ETMP
   */
  def submitReturn(
                    returnCreateRequest: ReturnCreateRequest,
                    vpdId: VpdId,
                    periodKey: PeriodKey
                  )(implicit hc: HeaderCarrier, request: IdentifierRequest[?]): Future[ReturnSubmittedResponse] = {
    // Submit to ETMP first - this is the primary operation
    submitReturnsConnector.submitReturn(returnCreateRequest, vpdId)
      .map { successResponse =>
        // After successful ETMP submission, queue NRS work item for background processing if enabled
        if (appConfig.nrsGenerationEnabled) {
          // This is fire-and-forget - we don't wait for the result
          val nrsPayload = Json.toJson(returnCreateRequest)

          // Queue work item - NRS submission will be processed by scheduler
          nrsService.makeWorkItemAndQueue(nrsPayload, NOTABLE_EVENT_SUBMIT_RETURN, returnCreateRequest.periodKey)
            .recover { case ex =>
              // Log NRS queueing failures but don't affect the returns submission response
              logger.warn(s"Failed to queue NRS work item for vpdId: $vpdId, periodKey: $periodKey", ex)
              ()
            }
        } else {
          logger.info(s"NRS generation disabled - skipping for vpdId: $vpdId, periodKey: $periodKey")
        }

        // Return success response immediately without waiting for NRS
        successResponse
      }
  }
}