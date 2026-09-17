/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingduty.models

import play.api.libs.json.*
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.crypto.CryptoProvider

import java.time.Instant

class UserAnswersSpec extends SpecBase {
  
  private val returnsUserAnswers = UserAnswers(
    vpdId = vpdId.id,
    periodKey = periodKey.value,
    startedTime = Instant.parse("2026-04-16T13:22:11.503Z"),
    lastUpdated = Instant.parse("2026-04-16T13:22:11.503Z")
  )

  implicit val crypto: Encrypter with Decrypter = CryptoProvider(appConfig).getCrypto

  "UserAnswers" - {
    "httpFormat" - {
      val json = Json.toJson(returnsUserAnswers)(UserAnswers.httpFormat).toString

      val errorJson =
        s"""{"vpdId":"${vpdId.id}","periodKey":"${periodKey.value}","lastUpdated":{"$$date":{"$$numberLong":"1718118467838"}}}"""

      "must show errors if json is not in the correct structure" in {
        val result = Json.parse(errorJson).validate[UserAnswers](UserAnswers.httpFormat)
        val errors = Seq[(JsPath, Seq[JsonValidationError])](
          (JsPath \ "startedTime", Seq(JsonValidationError("error.path.missing"))),
          (JsPath \ "data", Seq(JsonValidationError("error.path.missing")))
        )
        result mustBe JsError(errors)
      }

      "must serialise to json" in {
        Json.toJson(returnsUserAnswers)(UserAnswers.httpFormat).toString() mustBe json
      }

      "must deserialise from json" in {
        Json.parse(json).as[UserAnswers](UserAnswers.httpFormat) mustBe returnsUserAnswers
      }
    }

    "mongoFormat" - {
      val json = Json.toJson(returnsUserAnswers)(UserAnswers.mongoFormat).toString

      "must serialise to json with encrypted data field" in {
        val jsonValue = Json.toJson(returnsUserAnswers)(UserAnswers.mongoFormat)
        val dataField = (jsonValue \ "data").as[String]
        
        // Data should be encrypted (not a plain object)
        dataField must not be empty
        dataField must not include "{"
      }

      "must deserialise from json with encrypted data field" in {
        Json.parse(json).as[UserAnswers](UserAnswers.mongoFormat) mustBe returnsUserAnswers
      }
    }
  }
}
