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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingduty.connectors.SubmitReturnsConnector
import uk.gov.hmrc.vapingduty.models.identifiers.{PeriodKey, VpdId}
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest
import uk.gov.hmrc.vapingduty.models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ReturnsSubmissionService @Inject()(
                                          submitReturnsConnector: SubmitReturnsConnector
                                        ) extends Logging {

  def submitReturn(
                    returnCreateRequest: ReturnCreateRequest,
                    vpdId: VpdId,
                    periodKey: PeriodKey
                  )(using hc: HeaderCarrier, request: IdentifierRequest[?]): Future[ReturnSubmittedResponse] = {
    submitReturnsConnector.submitReturn(returnCreateRequest, vpdId)
  }
}
