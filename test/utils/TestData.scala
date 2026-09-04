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
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, User}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.vapingduty.models.*
import uk.gov.hmrc.vapingduty.models.identifiers.{InternalId, PeriodKey, VpdId}
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsMetadata}
import uk.gov.hmrc.vapingduty.models.returns.*
import uk.gov.hmrc.vapingduty.models.returns.submit.{ReturnCreateRequest, ReturnCreateResponse, ReturnSubmittedResponse}
import uk.gov.hmrc.vapingduty.models.returns.view.*

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

  val periodKey = PeriodKey("26AB")
  val vpdReferenceNumber = "XMVPD0000100021"
  val submissionId = "SUB123456789"
  val chargeReference = "CHG987654321"

  val nilReturnNoProducts: NilReturn = NilReturn(
    vapingProductsProduced = "No"
  )

  val regularReturn: RegularReturn = RegularReturn(
    taxType = "641",
    dutyRate = BigDecimal("2.20"),
    amountProducedLiquid = BigDecimal("1000.50"),
    dutyDue = BigDecimal("2201.10")
  )

  val totalDutyDue: TotalDutyDue = TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("3751.88"),
    totalDutyOverDeclaration = BigDecimal("100.00"),
    totalDutyUnderDeclaration = BigDecimal("50.00"),
    totalDutySpoiltProduct = BigDecimal("25.00"),
    totalDue = BigDecimal("3676.88")
  )

  val totalDutyDueNil: TotalDutyDue = TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("0.00"),
    totalDutyOverDeclaration = BigDecimal("0.00"),
    totalDutyUnderDeclaration = BigDecimal("0.00"),
    totalDutySpoiltProduct = BigDecimal("0.00"),
    totalDue = BigDecimal("0.00")
  )

  val vapingProductsProducedRegular: VapingProductsProduced = VapingProductsProduced(
    vapingProdManufactured = "1",
    returns = Seq(regularReturn)
  )

  val sampleDeclarationDetails: DeclarationDetails = DeclarationDetails(
    fullName = "John Doe",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "john.doe@example.com"
  )

  val returnsCreateRequest: ReturnCreateRequest = ReturnCreateRequest(
    periodKey = periodKey.value,
    vapingProductsProduced = vapingProductsProducedRegular,
    overDeclaration = Some(overDeclaration),
    underDeclaration = Some(underDeclaration),
    spoiltProduct = Some(spoiltProduct),
    totalDutyDue = Some(totalDutyDue),
    otherOptions = Some(otherOptions),
    declaration = sampleDeclarationDetails
  )

  val returnSubmittedResponse: ReturnSubmittedResponse = ReturnSubmittedResponse(
    processingDate = Instant.now(clock),
    vpdReferenceNumber = vpdReferenceNumber,
    submissionId = Some(submissionId),
    chargeReference = Some(chargeReference),
    amount = BigDecimal("3676.88"),
    paymentDueDate = Some(LocalDate.of(2026, 6, 30))
  )

  val returnCreateResponseSuccess: ReturnCreateResponse = ReturnCreateResponse(
    success = returnSubmittedResponse
  )

  // GET endpoint test data
  val idDetails: IdDetails = IdDetails(
    vpdReferenceNumber = vpdReferenceNumber,
    submissionId = submissionId
  )

  val chargeDetails: ChargeDetails = ChargeDetails(
    periodKey = periodKey.value,
    chargeReference = Some(chargeReference),
    periodFrom = LocalDate.of(2026, 6, 1),
    periodTo = LocalDate.of(2026, 6, 30),
    receiptDate = Instant.now(clock)
  )

  def overDeclarationProduct: OverDeclarationProduct = OverDeclarationProduct(
    returnPeriodAffected = "26AA",
    taxType = "641",
    dutyRate = BigDecimal("2.20"),
    amountOverDeclared = BigDecimal("50.00"),
    dutyDue = BigDecimal("110.00")
  )

  def overDeclaration: OverDeclaration = OverDeclaration(
    overDeclFilled = "1",
    reasonForOverDecl = Some("Correction needed"),
    overDeclarationProducts = Some(Seq(overDeclarationProduct))
  )

  def underDeclarationProduct: UnderDeclarationProduct = UnderDeclarationProduct(
    returnPeriodAffected = "26AA",
    taxType = "641",
    dutyRate = BigDecimal("2.20"),
    amountUnderDeclared = BigDecimal("25.00"),
    dutyDue = BigDecimal("55.00")
  )

  def underDeclaration: UnderDeclaration = UnderDeclaration(
    underDeclFilled = "1",
    reasonForUnderDecl = Some("Additional products found"),
    underDeclarationProducts = Some(Seq(underDeclarationProduct))
  )

  def spoiltProductItem: SpoiltProductItem = SpoiltProductItem(
    returnPeriodAffected = "26AA",
    taxType = "641",
    dutyRate = BigDecimal("2.20"),
    amountSpoilt = BigDecimal("10.00"),
    dutyDue = BigDecimal("-22.00")
  )

  def spoiltProduct: SpoiltProduct = SpoiltProduct(
    spoiltProductFilled = "1",
    spoiltProducts = Some(Seq(spoiltProductItem))
  )

  def otherOptions: OtherOptions = OtherOptions(
    vapingProductUnderDutySuspense = "1",
    volumeMovedFromDutySuspense = Some(BigDecimal("300.00")),
    volumeMovedToDutySuspense = Some(BigDecimal("150.00"))
  )

  val returnDisplaySuccess: ReturnDisplaySuccess = ReturnDisplaySuccess(
    processingDate = Instant.now(clock),
    idDetails = Some(idDetails),
    chargeDetails = Some(chargeDetails),
    vapingProductsProduced = Some(vapingProductsProducedRegular),
    overDeclaration = Some(overDeclaration),
    underDeclaration = Some(underDeclaration),
    spoiltProduct = Some(spoiltProduct),
    totalDutyDue = Some(totalDutyDue),
    otherOptions = Some(otherOptions),
    declaration = sampleDeclarationDetails
  )

  val returnDisplayResponse: ReturnDisplayResponse = ReturnDisplayResponse(
    success = returnDisplaySuccess
  )
  
  val sampleReturnCreateRequest: ReturnCreateRequest = returnsCreateRequest
  val sampleReturnSubmittedResponse: ReturnSubmittedResponse = returnSubmittedResponse
  val sampleReturnDisplaySuccess: ReturnDisplaySuccess = returnDisplaySuccess

  val sampleIdentityData: IdentityData = IdentityData(
    internalId = Some("test-internal-id"),
    optionalCredentials = Some(Credentials("test-cred-id", "GovernmentGateway")),
    confidenceLevel = ConfidenceLevel.L50,
    groupIdentifier = Some("test-group-id"),
    credentialRole = Some(User),
    affinityGroup = Some(AffinityGroup.Organisation),
    credentialStrength = Some("strong")
  )

  val sampleNrsMeta: NrsMetadata = NrsMetadata(
    businessId = "vpd",
    notableEvent = "vaping-duty-return",
    payloadContentType = "application/json",
    payloadSha256Checksum = "test-checksum",
    userSubmissionTimestamp = Instant.now(clock).toString,
    identityData = sampleIdentityData,
    userAuthToken = "test-auth-token",
    headerData = Map.empty[String, String],
    searchKeys = Map("zvpd" -> vpdId.id, "periodKey" -> periodKey.value)
  )
}
