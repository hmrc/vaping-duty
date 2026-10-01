package uk.gov.hmrc.vapingduty.models.returns.submit

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.vapingduty.base.SpecBase

import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.{LocalDate, LocalDateTime}
import scala.Some
import scala.language.postfixOps

class ReturnCreateResponseSpec extends SpecBase {

  "ReturnCreateResponse" - {
    "should parse example success from ETMP" in {

      val returnCreateResponseJsonFromQA: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDate": "2026-10-01T13:17:38Z",
          |    "vpdReferenceNumber": "GBWK5554441WK",
          |    "submissionId": 460000000127,
          |    "chargeReference": "XR002610246297",
          |    "amount": 22000,
          |    "paymentDueDate": "2026-11-15"
          |  }
          |}
          |""".stripMargin)

      returnCreateResponseJsonFromQA.as[ReturnCreateResponse] mustBe ReturnCreateResponse(
        success = ReturnSubmittedResponse(
          processingDate = LocalDateTime.parse("2026-10-01T13:17:38Z", ISO_OFFSET_DATE_TIME),
          vpdReferenceNumber = "GBWK5554441WK",
          submissionId = Some(460000000127L),
          chargeReference = Some("XR002610246297"),
          amount = 22000,
          paymentDueDate = Some(LocalDate.of(2026, 11, 15))
        )
      )
    }
  }
}
