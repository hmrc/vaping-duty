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
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.vapingduty.connectors.ObligationsConnector
import uk.gov.hmrc.vapingduty.controllers.actions.AuthorisedAction
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId

import scala.concurrent.ExecutionContext

class ObligationsController @Inject()(
                                       cc: ControllerComponents,
                                       connector: ObligationsConnector,
                                       authorise: AuthorisedAction
                                     )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def get(vpdId: VpdId): Action[AnyContent] = authorise.async { request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    connector.getObligations(vpdId).map(obligations => Ok(Json.toJson(obligations)))
      .recover { _ => InternalServerError("There was an issue retrieving obligations data") }
  }
}
