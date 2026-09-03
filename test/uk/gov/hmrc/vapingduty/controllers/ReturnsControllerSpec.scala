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
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.GetReturnsConnector
import uk.gov.hmrc.vapingduty.models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import uk.gov.hmrc.vapingduty.models.returns.view.ReturnDisplayResponse
import uk.gov.hmrc.vapingduty.services.ReturnsSubmissionService

import scala.concurrent.Future

class ReturnsControllerSpec extends SpecBase {

  val mockGetReturnsConnector: GetReturnsConnector = mock[GetReturnsConnector]
  val mockReturnsSubmissionService: ReturnsSubmissionService = mock[ReturnsSubmissionService]

  val controller = new ReturnsController(
    cc,
    mockGetReturnsConnector,
    fakeAuthorisedAction,
    mockReturnsSubmissionService
  )

  val returnCreateRequest: ReturnCreateRequest = sampleReturnCreateRequest

  "ReturnsController" - {
    "getReturn must" - {
      "return 200 OK with returns data when connector succeeds" in {
        reset(mockGetReturnsConnector)
        when(mockGetReturnsConnector.getReturn(eqTo(periodKey), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(returnDisplayResponse))

        val result = controller.getReturn(periodKey, vpdId)(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(returnDisplayResponse)
        verify(mockGetReturnsConnector, times(1)).getReturn(eqTo(periodKey), eqTo(vpdId))(any())
      }

      "return 500 INTERNAL_SERVER_ERROR when connector fails" in {
        reset(mockGetReturnsConnector)
        when(mockGetReturnsConnector.getReturn(eqTo(periodKey), eqTo(vpdId))(any()))
          .thenReturn(Future.failed(new RuntimeException("Connector error")))

        val result = controller.getReturn(periodKey, vpdId).apply(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockGetReturnsConnector, times(1)).getReturn(eqTo(periodKey), eqTo(vpdId))(any())
      }
    }

    "submitReturn must" - {
      "return 200 OK with submission response when service succeeds" in {
        reset(mockReturnsSubmissionService)
        when(mockReturnsSubmissionService.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId), eqTo(periodKey))(any(), any()))
          .thenReturn(Future.successful(returnSubmittedResponse))

        val result = controller.submitReturn(vpdId, periodKey).apply(fakeRequestWithJsonBody(Json.toJson(returnCreateRequest)))

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(returnSubmittedResponse)
        verify(mockReturnsSubmissionService, times(1)).submitReturn(eqTo(returnCreateRequest), eqTo(vpdId), eqTo(periodKey))(any(), any())
      }

      "return 500 INTERNAL_SERVER_ERROR when service fails" in {
        reset(mockReturnsSubmissionService)
        when(mockReturnsSubmissionService.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId), eqTo(periodKey))(any(), any()))
          .thenReturn(Future.failed(InternalServerException("Service error")))

        val result = controller.submitReturn(vpdId, periodKey).apply(fakeRequestWithJsonBody(Json.toJson(returnCreateRequest)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockReturnsSubmissionService, times(1)).submitReturn(eqTo(returnCreateRequest), eqTo(vpdId), eqTo(periodKey))(any(), any())
      }
    }
  }
}