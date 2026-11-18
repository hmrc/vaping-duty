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
import uk.gov.hmrc.vapingduty.connectors.ObligationsConnector
import uk.gov.hmrc.vapingduty.models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationsResponse}

import java.time.LocalDate
import scala.concurrent.Future

class ObligationsServiceSpec extends SpecBase {

  val mockObligationsConnector: ObligationsConnector = mock[ObligationsConnector]
  val service = new ObligationsService(appConfig, mockObligationsConnector)

  given HeaderCarrier = hc

  private def obligation(periodKey: String, status: String = "O") = ObligationDetails(
    openOrFulfilledStatus = status,
    iCFromDate            = LocalDate.of(2026, 1, 1),
    iCToDate              = LocalDate.of(2026, 1, 31),
    iCDateReceived        = None,
    iCDueDate             = LocalDate.of(2026, 2, 7),
    periodKey             = periodKey
  )

  private def item(referenceType: String, referenceNumber: String, details: ObligationDetails*) =
    ObligationItem(Some(Identification(referenceType, referenceNumber, None)), details)

  private def givenConnectorReturns(items: ObligationItem*) = {
    reset(mockObligationsConnector)
    when(mockObligationsConnector.getObligations(eqTo(vpdId))(any()))
      .thenReturn(Future.successful(ObligationsResponse(items)))
  }

  "getObligations must" - {
    "return the obligation details for the matching VPD identification" in {
      givenConnectorReturns(item("ZVPD", vpdId.id, obligation("26AA"), obligation("26AB", "F")))

      service.getObligations(vpdId).futureValue mustBe Seq(obligation("26AA"), obligation("26AB", "F"))
    }

    "exclude obligations for a different reference number" in {
      givenConnectorReturns(
        item("ZVPD", "GBWK0000000WK", obligation("25AL")),
        item("ZVPD", vpdId.id, obligation("26AA"))
      )

      service.getObligations(vpdId).futureValue mustBe Seq(obligation("26AA"))
    }

    "exclude obligations for a different reference type" in {
      givenConnectorReturns(
        item("NINO", vpdId.id, obligation("25AL")),
        item("ZVPD", vpdId.id, obligation("26AA"))
      )

      service.getObligations(vpdId).futureValue mustBe Seq(obligation("26AA"))
    }

    "exclude obligations without an identification" in {
      givenConnectorReturns(
        ObligationItem(None, Seq(obligation("25AL"))),
        item("ZVPD", vpdId.id, obligation("26AA"))
      )

      service.getObligations(vpdId).futureValue mustBe Seq(obligation("26AA"))
    }

    "flatten multiple matching items into a single list, preserving order" in {
      givenConnectorReturns(
        item("ZVPD", vpdId.id, obligation("26AA"), obligation("26AB")),
        item("IOSS", "IM8260001234", obligation("25AL")),
        item("ZVPD", vpdId.id, obligation("26AC"))
      )

      service.getObligations(vpdId).futureValue mustBe Seq(obligation("26AA"), obligation("26AB"), obligation("26AC"))
    }

    "return an empty list when there are no matching obligations" in {
      givenConnectorReturns(item("NINO", "AB123456C", obligation("25AL")))

      service.getObligations(vpdId).futureValue mustBe Seq.empty
    }

    "return an empty list when the response contains no obligations" in {
      givenConnectorReturns()

      service.getObligations(vpdId).futureValue mustBe Seq.empty
    }

    "fail when the connector fails" in {
      reset(mockObligationsConnector)
      when(mockObligationsConnector.getObligations(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(InternalServerException("Failed to get obligations")))

      whenReady(service.getObligations(vpdId).failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage mustBe "Failed to get obligations"
      }
    }
  }
}
