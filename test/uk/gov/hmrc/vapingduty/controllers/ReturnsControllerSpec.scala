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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.ReturnsConnector

import scala.concurrent.Future

class ReturnsControllerSpec extends SpecBase {

  val mockConnector: ReturnsConnector = mock[ReturnsConnector]

  val controller = new ReturnsController(
    cc,
    mockConnector,
    fakeAuthorisedAction
  )

  "submitReturn must" - {
    "return 200 OK when the connector successfully submits returns" in {
      when(mockConnector.submitReturn(eqTo(returnCreateRequestRegular), eqTo(vpdId))(any()))
        .thenReturn(Future.successful(returnCreateResponseSuccess.success))

      val result = controller.submitReturn(vpdId)(fakeRequestWithJsonBody(Json.toJson(returnCreateRequestRegular)))

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(returnCreateResponseSuccess.success)
    }

    "return 200 OK when the connector successfully submits nil return" in {
      when(mockConnector.submitReturn(eqTo(returnCreateRequestNil), eqTo(vpdId))(any()))
        .thenReturn(Future.successful(returnCreateResponseMinimal.success))

      val result = controller.submitReturn(vpdId)(fakeRequestWithJsonBody(Json.toJson(returnCreateRequestNil)))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(returnCreateResponseMinimal.success)
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector fails" in {
      val errorMessage = "Failed to submit VPD return"
      when(mockConnector.submitReturn(eqTo(returnCreateRequestRegular), eqTo(vpdId))(any()))
        .thenReturn(Future.failed(InternalServerException(errorMessage)))

      val result = controller.submitReturn(vpdId)(fakeRequestWithJsonBody(Json.toJson(returnCreateRequestRegular)))

      status(result)          mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe errorMessage
    }
  }
}
