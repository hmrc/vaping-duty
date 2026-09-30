/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.vapingduty.connectors.helpers

import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.models.ConnectorErrorResponse

import java.time.{Instant, LocalDate}
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

class UnprocessableEntityLoggingSpec extends SpecBase {

  "UnprocessableEntityLogging" - {
    "should parse a valid Obligations 422 error body" in {
      new UnprocessableEntityLogging {}.parseErrorMessage(body =
        """
          |{
          | "errors":{
          |   "processingDate":"2026-09-30T09:42:05Z",
          |   "code":"025",
          |   "text":"No associated data found."
          | }
          |}
          |""".stripMargin  
      ) mustBe Some(
        ConnectorErrorResponse(
          Instant.parse("2026-09-30T09:42:05Z"),
          code = "025",
          text = "No associated data found."))
    }
  }

}
