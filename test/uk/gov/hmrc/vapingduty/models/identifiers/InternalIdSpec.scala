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

package uk.gov.hmrc.vapingduty.models.identifiers

import uk.gov.hmrc.vapingduty.base.SpecBase
import play.api.libs.json.Json

class InternalIdSpec extends SpecBase {

  private val internalId: InternalId = InternalId("Int-1-2-3")

  "InternalId" - {
    val json = s"""{"id":"$internalId"}"""

    "must serialise to json" in {
      Json.toJson(internalId).toString mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[InternalId] mustBe internalId
    }

    "fail to bind an invalid InternalId" in {
      val binder = InternalId.given_PathBindable_InternalId
      val result = binder.bind("id", "tooShort")
      result mustBe Left("Invalid InternalId") // Assumes your Left value
    }
  }
}
