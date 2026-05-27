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
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingduty.controllers.actions.AuthorisedAction
import uk.gov.hmrc.vapingduty.models.identifiers.{PeriodKey, VpdId}
import uk.gov.hmrc.vapingduty.models.{UpdateFailure, UpdateSuccess, UserAnswers}
import uk.gov.hmrc.vapingduty.repositories.UserAnswersRepository

import scala.concurrent.{ExecutionContext, Future}

class UserAnswersController @Inject()(
                                       cc: ControllerComponents,
                                       userAnswersRepository: UserAnswersRepository,
                                       authorise: AuthorisedAction
                                     )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getUserAnswers(vpdId: VpdId, periodKey: PeriodKey): Action[AnyContent] = authorise.async { _ =>
    userAnswersRepository.get(vpdId, periodKey).map {
      case Some(ua) => Ok(Json.toJson(ua))
      case None     => NotFound
    }
  }

  def set(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body.validate[UserAnswers] match
        case JsSuccess(ua, _) =>
          println(ua)
          userAnswersRepository.set(ua).map {
          case UpdateSuccess => NoContent
          case UpdateFailure => NotModified
        }
        case JsError(errors) =>
          println("ERRORS&&&&&&&&&&" + errors)
          Future.successful(BadRequest)
  }

  def clear(vpdId: VpdId, periodKey: PeriodKey): Action[AnyContent] = authorise.async {
    userAnswersRepository.clear(vpdId, periodKey).map {
      case UpdateSuccess => NoContent
      case UpdateFailure => NotModified
    }
  }

  def keepAlive(vpdId: VpdId, periodKey: PeriodKey): Action[AnyContent] = authorise.async {
    userAnswersRepository.keepAlive(vpdId, periodKey).map {
      case UpdateSuccess => NoContent
      case UpdateFailure => NotModified
    }
  }
}
