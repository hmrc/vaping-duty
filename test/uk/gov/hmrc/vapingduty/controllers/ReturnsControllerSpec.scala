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
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.{GetReturnsConnector, SubmitReturnsConnector}
import uk.gov.hmrc.vapingduty.models.returns.VapingProductsProduced

import scala.concurrent.Future

class ReturnsControllerSpec extends SpecBase {

  val mockSubmitConnector: SubmitReturnsConnector = mock[SubmitReturnsConnector]
  val mockGetConnector: GetReturnsConnector = mock[GetReturnsConnector]

  val controller = new ReturnsController(
    cc,
    mockSubmitConnector,
    mockGetConnector,
    fakeAuthorisedAction
  )

  "submitReturn must" - {
    "return 200 OK when the connector successfully submits returns" in {
      when(mockSubmitConnector.submitReturn(eqTo(returnsCreateRequest), eqTo(vpdId))(any()))
        .thenReturn(Future.successful(returnCreateResponseSuccess.success))

      val result = controller.submitReturn(vpdId, periodKey)(fakeRequestWithJsonBody(Json.toJson(returnsCreateRequest)))

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(returnCreateResponseSuccess.success)
    }

    "return 200 OK when the connector successfully submits nil return" in {
      val nilRequestBody = returnsCreateRequest.copy(
        vapingProductsProduced = VapingProductsProduced(
          nilReturn = Seq(nilReturnNoProducts),
          regularReturn = Seq.empty
        ),
        totalDutyDue = totalDutyDueNil
      )
      
      val nilReturn = returnCreateResponseSuccess.success.copy(
        submissionID = None,
        chargeReference = None,
        amount = BigDecimal("0.0"),
        paymentDueDate = None,
      )

      when(mockSubmitConnector.submitReturn(eqTo(nilRequestBody), eqTo(vpdId))(any()))
        .thenReturn(Future.successful(nilReturn))

      val result = controller.submitReturn(vpdId, periodKey)(fakeRequestWithJsonBody(Json.toJson(nilRequestBody)))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(nilReturn)
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector fails" in {
      when(mockSubmitConnector.submitReturn(eqTo(returnsCreateRequest), eqTo(vpdId))(any()))
        .thenReturn(Future.failed(InternalServerException("")))

      val result = controller.submitReturn(vpdId, periodKey)(fakeRequestWithJsonBody(Json.toJson(returnsCreateRequest)))

      status(result)          mustBe INTERNAL_SERVER_ERROR
    }
  }
  "GetReturns must" - {
    "return 200 OK when the connector successfully gets returns" in {
      when(mockGetConnector.getReturn(eqTo(periodKey), eqTo(vpdId))(any()))
        .thenReturn(Future.successful(returnDisplayResponse))

      val result = controller.getReturn(periodKey, vpdId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(returnDisplayResponse)
    }

    "return 500 INTERNAL_SERVER_ERROR when the connector fails" in {
      when(mockGetConnector.getReturn(eqTo(periodKey), eqTo(vpdId))(any()))
        .thenReturn(Future.failed(InternalServerException("")))

      val result = controller.getReturn(periodKey, vpdId)(fakeRequest)

      status(result)          mustBe INTERNAL_SERVER_ERROR
    }
  }
}
