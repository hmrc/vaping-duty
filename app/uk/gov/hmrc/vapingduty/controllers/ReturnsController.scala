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

package uk.gov.hmrc.vapingduty.controllers

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.connectors.{GetReturnsConnector, SubmitReturnsConnector}
import uk.gov.hmrc.vapingduty.controllers.actions.AuthorisedAction
import uk.gov.hmrc.vapingduty.models.identifiers.{PeriodKey, VpdId}
import uk.gov.hmrc.vapingduty.models.nrs.NrsMetadata
import uk.gov.hmrc.vapingduty.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingduty.services.NrsService

import scala.concurrent.ExecutionContext

class ReturnsController @Inject()(
                                   cc: ControllerComponents,
                                   submitConnector: SubmitReturnsConnector,
                                   getConnector: GetReturnsConnector,
                                   authorise: AuthorisedAction,
                                   nrsService: NrsService,
                                   appConfig: AppConfig
                                 )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  private val NOTABLE_EVENT_SUBMIT_RETURN = NrsMetadata.notableEventSubmitReturn

  def getReturn(periodKey: PeriodKey, vpdId: VpdId): Action[AnyContent] = authorise.async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    getConnector.getReturn(periodKey, vpdId)
      .map(returns => Ok(Json.toJson(returns)))
      .recover { _ => InternalServerError("There was an issue retrieving returns data") }

  }
  
  def submitReturn(vpdId: VpdId, periodKey: PeriodKey): Action[JsValue] = {
    authorise(parse.json).async { implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequest(request = request.request)

      withJsonBody[ReturnCreateRequest] { returnSubmission =>
        // Submit to ETMP first - this is the primary operation
        submitConnector.submitReturn(returnSubmission, vpdId)
          .map { successResponse =>
            // After successful ETMP submission, queue NRS work item for background processing if enabled
            if (appConfig.nrsSubmissionEnabled) {
              // This is fire-and-forget - we don't wait for the result
              val nrsPayload = Json.toJson(returnSubmission)
              
              // Queue work item - NRS submission will be processed by scheduler
              nrsService.makeWorkItemAndQueue(nrsPayload, NOTABLE_EVENT_SUBMIT_RETURN, returnSubmission.periodKey)
                .recover { case ex =>
                  // Log NRS queueing failures but don't affect the returns submission response
                  logger.warn(s"Failed to queue NRS work item for vpdId: $vpdId, periodKey: $periodKey", ex)
                  ()
                }
            } else {
              logger.info(s"NRS submission disabled - skipping for vpdId: $vpdId, periodKey: $periodKey")
            }
            
            // Return success response immediately without waiting for NRS
            Ok(Json.toJson(successResponse))
          }
          .recover { _ =>
            // ETMP submission failed - don't attempt NRS submission
            logger.error(s"Failed to submit return for vpdId: $vpdId, periodKey: $periodKey")
            InternalServerError
          }
      }
    }
  }
}
