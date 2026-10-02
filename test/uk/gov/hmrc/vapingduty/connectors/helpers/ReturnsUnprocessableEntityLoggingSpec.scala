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

import java.time.Instant

class ReturnsUnprocessableEntityLoggingSpec extends SpecBase {

  "UnprocessableEntityLogging" - {

    "should parse a valid POST Returns 422 error body" in {
      new ReturnsUnprocessableEntityLogging {}.parseReturnsErrorMessage(body =
        """
          |{
          |  "error": {
          |    "code": "001",
          |    "processingDate": "2022-01-31T09:26:17Z",
          |    "text": "REGIME missing or invalid"
          |  }
          |}
          |""".stripMargin  
      ) mustBe Some(
        ConnectorErrorResponse(
          Instant.parse("2022-01-31T09:26:17Z"),
          code = "001",
          text = "REGIME missing or invalid"))
    }

    "should parse a valid GET Returns 422 error body" in {
      new ReturnsUnprocessableEntityLogging {}.parseReturnsErrorMessage(body =
        """
          |{
          |  "error": {
          |    "code": "002",
          |    "processingDate": "2026-01-31T09:26:17Z",
          |    "text": "ID Number missing or invalid"
          |  }
          |}
          |""".stripMargin  
      ) mustBe Some(
        ConnectorErrorResponse(
          Instant.parse("2026-01-31T09:26:17Z"),
          code = "002",
          text = "ID Number missing or invalid"))
    }
  }

}
