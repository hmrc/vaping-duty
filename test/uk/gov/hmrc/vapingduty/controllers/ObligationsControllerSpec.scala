/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.*
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.ObligationsConnector
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.obligations.ObligationsResponse

import scala.concurrent.Future

class ObligationsControllerSpec extends SpecBase {
  private val vpdId = VpdId("vpdId")

  val mockConnector: ObligationsConnector = mock[ObligationsConnector]

  val controller = new ObligationsController(
    cc,
    mockConnector,
    fakeAuthorisedAction
  )

  "get must" - {
    "return OK with obligation data" in {

      when(mockConnector.getObligations(any())(any())).thenReturn(Future.successful(ObligationsResponse(Seq.empty)))

      val result: Future[Result] = controller.get(vpdId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(ObligationsResponse(Seq.empty))
    }

    "return INTERNAL_SERVER_ERROR when the connector fails" in {

      when(mockConnector.getObligations(any())(any())).thenReturn(Future.failed(InternalServerException("")))

      val result: Future[Result] = controller.get(vpdId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
