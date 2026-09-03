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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.connectors.SubmitReturnsConnector
import uk.gov.hmrc.vapingduty.models.nrs.NrsMetadata
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest
import uk.gov.hmrc.vapingduty.models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}

import scala.concurrent.Future

class ReturnsSubmissionServiceSpec extends SpecBase with ScalaFutures {

  val mockSubmitReturnsConnector: SubmitReturnsConnector = mock[SubmitReturnsConnector]
  val mockNrsService: NrsService = mock[NrsService]
  val mockAppConfig: AppConfig = mock[AppConfig]

  val service = new ReturnsSubmissionService(
    mockSubmitReturnsConnector,
    mockNrsService,
    mockAppConfig
  )
  
  val returnCreateRequest: ReturnCreateRequest = sampleReturnCreateRequest

  given HeaderCarrier = hc
  given IdentifierRequest[?] = IdentifierRequest(fakeRequest, vpdId.id, internalId.id)

  "ReturnsSubmissionService" - {
    "submitReturn must" - {
      "successfully submit to ETMP and queue NRS work item when NRS is enabled" in {
        reset(mockSubmitReturnsConnector, mockNrsService, mockAppConfig)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)
        when(mockSubmitReturnsConnector.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(returnSubmittedResponse))
        when(mockNrsService.makeWorkItemAndQueue(any[JsValue], eqTo(NrsMetadata.notableEventSubmitReturn), eqTo(returnCreateRequest.periodKey))(using any(), any()))
          .thenReturn(Future.successful(()))

        val result = service.submitReturn(returnCreateRequest, vpdId, periodKey)

        whenReady(result) { response =>
          response mustBe returnSubmittedResponse
          verify(mockSubmitReturnsConnector, times(1)).submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any())
          verify(mockNrsService, times(1)).makeWorkItemAndQueue(any[JsValue], eqTo(NrsMetadata.notableEventSubmitReturn), eqTo(returnCreateRequest.periodKey))(using any(), any())
        }
      }

      "successfully submit to ETMP and skip NRS when NRS is disabled" in {
        reset(mockSubmitReturnsConnector, mockNrsService, mockAppConfig)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(false)
        when(mockSubmitReturnsConnector.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(returnSubmittedResponse))

        val result = service.submitReturn(returnCreateRequest, vpdId, periodKey)

        whenReady(result) { response =>
          response mustBe returnSubmittedResponse
          verify(mockSubmitReturnsConnector, times(1)).submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any())
          verify(mockNrsService, never()).makeWorkItemAndQueue(any(), any(), any())(using any(), any())
        }
      }

      "successfully submit to ETMP even when NRS queueing fails" in {
        reset(mockSubmitReturnsConnector, mockNrsService, mockAppConfig)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)
        when(mockSubmitReturnsConnector.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(returnSubmittedResponse))
        when(mockNrsService.makeWorkItemAndQueue(any[JsValue], eqTo(NrsMetadata.notableEventSubmitReturn), eqTo(returnCreateRequest.periodKey))(using any(), any()))
          .thenReturn(Future.failed(new RuntimeException("NRS queueing failed")))

        val result = service.submitReturn(returnCreateRequest, vpdId, periodKey)

        whenReady(result) { response =>
          response mustBe returnSubmittedResponse
          verify(mockSubmitReturnsConnector, times(1)).submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any())
          verify(mockNrsService, times(1)).makeWorkItemAndQueue(any[JsValue], eqTo(NrsMetadata.notableEventSubmitReturn), eqTo(returnCreateRequest.periodKey))(using any(), any())
        }
      }

      "fail when ETMP submission fails" in {
        reset(mockSubmitReturnsConnector, mockNrsService, mockAppConfig)
        when(mockSubmitReturnsConnector.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any()))
          .thenReturn(Future.failed(InternalServerException("ETMP submission failed")))

        val result = service.submitReturn(returnCreateRequest, vpdId, periodKey)

        whenReady(result.failed) { exception =>
          exception mustBe an[InternalServerException]
          verify(mockSubmitReturnsConnector, times(1)).submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any())
          verify(mockNrsService, never()).makeWorkItemAndQueue(any(), any(), any())(using any(), any())
        }
      }
    }
  }
}