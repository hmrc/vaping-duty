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

package utils

import org.scalacheck.Gen
import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.vapingduty.models.*
import uk.gov.hmrc.vapingduty.models.identifiers.{InternalId, VpdId}
import uk.gov.hmrc.vapingduty.models.returns.*

import java.time.*

trait TestData {

  def vpdIdGen: Gen[String] = Gen.listOfN(7, Gen.numChar).map(id => s"GBWK${id.mkString}WK")

  val clockMillis: Long = 1718118467838L
  val clock: Clock      = Clock.fixed(Instant.ofEpochMilli(clockMillis), ZoneId.of("UTC"))

  val dummyUUID = "01234567-89ab-cdef-0123-456789abcdef"

  val regime: String           = "ZVPD"
  val vpdId: VpdId             = VpdId(id = vpdIdGen.sample.get)
  val internalId: InternalId   = InternalId(id = "internalId")

  case class DownstreamErrorDetails(code: String, message: String, logID: String)

  object DownstreamErrorDetails {
    implicit val downstreamErrorDetailsWrites: OFormat[DownstreamErrorDetails] = Json.format[DownstreamErrorDetails]
  }

  val badRequest          = DownstreamErrorDetails("400", "You messed up", "id")
  val unprocessable       = DownstreamErrorDetails("422", "Unprocessable", "id")
  val internalServerError = DownstreamErrorDetails("500", "Computer says No!", "id")

  val periodKey = "26AB"
  val vpdReferenceNumber = "XMVPD0000100021"
  val submissionId = "SUB123456789"
  val chargeReference = "CHG987654321"

  val nilReturnNoProducts: NilReturn = NilReturn(
    vapingProductsProduced = "No"
  )

  val regularReturnStandard: RegularReturn = RegularReturn(
    taxType = "351",
    dutyRate = BigDecimal("2.50"),
    amountProducedLiquid = BigDecimal("1000.50"),
    amountProducedSolid = BigDecimal("500.25"),
    dutyDue = BigDecimal("3751.88")
  )

  val regularReturnHighDuty: RegularReturn = RegularReturn(
    taxType = "352",
    dutyRate = BigDecimal("5.00"),
    amountProducedLiquid = BigDecimal("2000.00"),
    amountProducedSolid = BigDecimal("1000.00"),
    dutyDue = BigDecimal("15000.00")
  )

  val regularReturnLowDuty: RegularReturn = RegularReturn(
    taxType = "353",
    dutyRate = BigDecimal("1.25"),
    amountProducedLiquid = BigDecimal("100.00"),
    amountProducedSolid = BigDecimal("50.00"),
    dutyDue = BigDecimal("187.50")
  )

  val totalDutyDueNil: TotalDutyDue = TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("0.00"),
    totalDutyOverDeclaration = BigDecimal("0.00"),
    totalDutyUnderDeclaration = BigDecimal("0.00"),
    totalDutySpoiltProduct = BigDecimal("0.00"),
    adjustmentAmount = BigDecimal("0.00"),
    totalDutyDue = BigDecimal("0.00")
  )

  val totalDutyDueStandard: TotalDutyDue = TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("3751.88"),
    totalDutyOverDeclaration = BigDecimal("100.00"),
    totalDutyUnderDeclaration = BigDecimal("50.00"),
    totalDutySpoiltProduct = BigDecimal("25.00"),
    adjustmentAmount = BigDecimal("-75.00"),
    totalDutyDue = BigDecimal("3676.88")
  )

  val totalDutyDueWithAdjustments: TotalDutyDue = TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("15187.50"),
    totalDutyOverDeclaration = BigDecimal("500.00"),
    totalDutyUnderDeclaration = BigDecimal("200.00"),
    totalDutySpoiltProduct = BigDecimal("100.00"),
    adjustmentAmount = BigDecimal("-400.00"),
    totalDutyDue = BigDecimal("14787.50")
  )

  val vapingProductsProducedNil: VapingProductsProduced = VapingProductsProduced(
    nilReturn = Seq(nilReturnNoProducts),
    regularReturn = Seq.empty
  )

  val vapingProductsProducedRegular: VapingProductsProduced = VapingProductsProduced(
    nilReturn = Seq.empty,
    regularReturn = Seq(regularReturnStandard)
  )

  val vapingProductsProducedMultiple: VapingProductsProduced = VapingProductsProduced(
    nilReturn = Seq.empty,
    regularReturn = Seq(regularReturnStandard, regularReturnHighDuty, regularReturnLowDuty)
  )

  val vapingProductsProducedMixed: VapingProductsProduced = VapingProductsProduced(
    nilReturn = Seq(nilReturnNoProducts),
    regularReturn = Seq(regularReturnStandard, regularReturnHighDuty)
  )

  val returnCreateRequestNil: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = periodKey,
    vapingProductsProduced = vapingProductsProducedNil,
    totalDutyDue = totalDutyDueNil
  )

  val returnCreateRequestRegular: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = periodKey,
    vapingProductsProduced = vapingProductsProducedRegular,
    totalDutyDue = totalDutyDueStandard
  )

  val returnCreateRequestMultiple: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = periodKey,
    vapingProductsProduced = vapingProductsProducedMultiple,
    totalDutyDue = totalDutyDueWithAdjustments
  )

  val returnCreateRequestMixed: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = periodKey,
    vapingProductsProduced = vapingProductsProducedMixed,
    totalDutyDue = totalDutyDueWithAdjustments
  )

  val returnSubmittedResponseFull: ReturnSubmittedResponse = ReturnSubmittedResponse(
    processingDate = Instant.now(clock),
    vpdReferenceNumber = vpdReferenceNumber,
    submissionID = Some(submissionId),
    chargeReference = Some(chargeReference),
    amount = BigDecimal("3676.88"),
    paymentDueDate = Some(LocalDate.of(2026, 6, 30))
  )

  val returnSubmittedResponseMinimal: ReturnSubmittedResponse = ReturnSubmittedResponse(
    processingDate = Instant.now(clock),
    vpdReferenceNumber = vpdReferenceNumber,
    submissionID = None,
    chargeReference = None,
    amount = BigDecimal("0.00"),
    paymentDueDate = None
  )

  val returnSubmittedResponseWithPayment: ReturnSubmittedResponse = ReturnSubmittedResponse(
    processingDate = Instant.now(clock),
    vpdReferenceNumber = vpdReferenceNumber,
    submissionID = Some(submissionId),
    chargeReference = Some(chargeReference),
    amount = BigDecimal("14787.50"),
    paymentDueDate = Some(LocalDate.of(2026, 7, 31))
  )

  val returnCreateResponseSuccess: ReturnCreateResponse = ReturnCreateResponse(
    success = returnSubmittedResponseFull
  )

  val returnCreateResponseMinimal: ReturnCreateResponse = ReturnCreateResponse(
    success = returnSubmittedResponseMinimal
  )

  val returnCreateResponseWithPayment: ReturnCreateResponse = ReturnCreateResponse(
    success = returnSubmittedResponseWithPayment
  )
}
