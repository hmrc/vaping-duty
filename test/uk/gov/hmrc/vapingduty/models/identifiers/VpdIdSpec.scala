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

import play.api.libs.json.Json
import uk.gov.hmrc.vapingduty.base.SpecBase
import utils.TestData

class VpdIdSpec extends SpecBase with TestData {

  "VpdId" - {
    val json = s"""{"id":"$vpdId"}"""

    "must serialise to json" in {
      Json.toJson(vpdId).toString mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[VpdId] mustBe vpdId
    }

    "fail to bind an invalid VPD ID" in {
      val binder = VpdId.given_PathBindable_VpdId
      val result = binder.bind("id", "invalidVpdId")
      result mustBe Left("Invalid VpdId") // Assumes your Left value
    }
  }
}
