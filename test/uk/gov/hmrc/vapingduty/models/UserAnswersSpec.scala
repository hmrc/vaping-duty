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

import uk.gov.hmrc.vapingduty.models.identifiers.InternalId
import uk.gov.hmrc.vapingduty.base.SpecBase
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError, JsObject}

import java.time.Instant

class UserAnswersSpec extends SpecBase {

  private val internalId: InternalId = InternalId("Int-4435-242342-dsfsdf-5345")

  private val returnsUserAnswers = UserAnswers(
    id = internalId.toString,
    startedTime = Instant.parse("2026-04-16T13:22:11.503Z"),
    lastUpdated = Instant.parse("2026-04-16T13:22:11.503Z")
  )

  "UserAnswers" - {
    val json = Json.toJson(returnsUserAnswers).toString

    val errorJson =
      s"""{"_id":"$internalId","lastUpdated":{"$$date":{"$$numberLong":"1718118467838"}}}"""

    "must show errors if json is not in the correct structure" in {
      val result = Json.parse(errorJson).validate[UserAnswers]
      val errors = Seq[(JsPath, Seq[JsonValidationError])](
        (JsPath \ "startedTime", Seq(JsonValidationError("error.path.missing")))
      )
      result mustBe JsError(errors)
    }

    "must serialise to json" in {
      Json.toJson(returnsUserAnswers).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[UserAnswers] mustBe returnsUserAnswers
    }
  }
}
