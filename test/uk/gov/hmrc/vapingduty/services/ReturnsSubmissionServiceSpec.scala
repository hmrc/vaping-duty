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
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.connectors.SubmitReturnsConnector
import uk.gov.hmrc.vapingduty.models.requests.IdentifierRequest
import scala.concurrent.Future

class ReturnsSubmissionServiceSpec extends SpecBase {

  val mockSubmitReturnsConnector: SubmitReturnsConnector = mock[SubmitReturnsConnector]
  val service = new ReturnsSubmissionService(mockSubmitReturnsConnector)
  val returnCreateRequest = sampleReturnCreateRequest

  given HeaderCarrier = hc
  given IdentifierRequest[?] = IdentifierRequest(fakeRequest, vpdId.id, internalId.id)

  "submitReturn must" - {
    "successfully submit to ETMP" in {
      reset(mockSubmitReturnsConnector)
      when(mockSubmitReturnsConnector.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any()))
        .thenReturn(Future.successful(returnSubmittedResponse))

      val result = service.submitReturn(returnCreateRequest, vpdId, periodKey)

      whenReady(result) { response =>
        response mustBe returnSubmittedResponse
        verify(mockSubmitReturnsConnector, times(1)).submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any())
      }
    }

    "fail when ETMP submission fails" in {
      reset(mockSubmitReturnsConnector)
      when(mockSubmitReturnsConnector.submitReturn(eqTo(returnCreateRequest), eqTo(vpdId))(any()))
        .thenReturn(Future.failed(InternalServerException("ETMP submission failed")))

      val result = service.submitReturn(returnCreateRequest, vpdId, periodKey)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }
}
