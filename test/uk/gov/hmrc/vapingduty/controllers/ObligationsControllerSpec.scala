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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.*
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.obligations.ObligationDetails
import uk.gov.hmrc.vapingduty.services.ObligationsService

import java.time.LocalDate
import scala.concurrent.Future

class ObligationsControllerSpec extends SpecBase {
  private val vpdId = VpdId("vpdId")

  val mockObligationsService: ObligationsService = mock[ObligationsService]

  val controller = new ObligationsController(
    cc,
    mockObligationsService,
    fakeAuthorisedAction
  )

  private val obligation = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate            = LocalDate.of(2026, 1, 1),
    iCToDate              = LocalDate.of(2026, 1, 31),
    iCDateReceived        = None,
    iCDueDate             = LocalDate.of(2026, 2, 7),
    periodKey             = "26AA"
  )

  "get must" - {
    "return OK with the obligations as a list" in {

      when(mockObligationsService.getObligations(any())(using any())).thenReturn(Future.successful(Seq(obligation)))

      val result: Future[Result] = controller.get(vpdId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.parse(
        """[{"openOrFulfilledStatus":"O","iCFromDate":"2026-01-01","iCToDate":"2026-01-31","iCDueDate":"2026-02-07","periodKey":"26AA"}]"""
      )
    }

    "return OK with an empty list when there are no obligations" in {

      when(mockObligationsService.getObligations(any())(using any())).thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.get(vpdId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.arr()
    }

    "allow failures from the service to pass to the framework for logging and handling" in {

      when(mockObligationsService.getObligations(any())(using any()))
        .thenReturn(Future.failed[Seq[ObligationDetails]](InternalServerException("Failed to get Obligations")))

      val result: Future[Result] = controller.get(vpdId)(fakeRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage mustBe "Failed to get Obligations"
      }
    }
  }
}
