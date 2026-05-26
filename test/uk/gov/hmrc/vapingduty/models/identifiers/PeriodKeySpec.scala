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

package uk.gov.hmrc.vapingduty.models.identifiers

import play.api.libs.json.{JsString, Json}
import play.api.mvc.PathBindable
import uk.gov.hmrc.vapingduty.base.SpecBase

class PeriodKeySpec extends SpecBase {

  "PeriodKey" - {

    "must serialize to and from JSON" in {
      val periodKey = PeriodKey("24AA")
      val json      = Json.toJson(periodKey)
      
      json mustBe Json.obj("value" -> "24AA")
      json.as[PeriodKey] mustBe periodKey
    }

    "must serialize as a simple string value" in {
      val periodKey = PeriodKey("24AB")
      val json      = Json.toJson(periodKey)
      
      (json \ "value").as[String] mustBe "24AB"
    }

    "PathBindable" - {

      val pathBindable = summon[PathBindable[PeriodKey]]

      "must bind valid period keys" - {

        "for January (AA)" in {
          val result = pathBindable.bind("periodKey", "24AA")
          result mustBe Right(PeriodKey("24AA"))
        }

        "for February (AB)" in {
          val result = pathBindable.bind("periodKey", "24AB")
          result mustBe Right(PeriodKey("24AB"))
        }

        "for December (AL)" in {
          val result = pathBindable.bind("periodKey", "24AL")
          result mustBe Right(PeriodKey("24AL"))
        }

        "for different years" in {
          val result23 = pathBindable.bind("periodKey", "23AA")
          val result25 = pathBindable.bind("periodKey", "25AA")
          
          result23 mustBe Right(PeriodKey("23AA"))
          result25 mustBe Right(PeriodKey("25AA"))
        }
      }

      "must reject invalid period keys" - {

        "when too short" in {
          val result = pathBindable.bind("periodKey", "24A")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }

        "when too long" in {
          val result = pathBindable.bind("periodKey", "24AAA")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }

        "when lowercase letters" in {
          val result = pathBindable.bind("periodKey", "24aa")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }

        "when mixed case" in {
          val result = pathBindable.bind("periodKey", "24Aa")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }

        "when contains non-alphanumeric characters" in {
          val result = pathBindable.bind("periodKey", "24-A")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }

        "when year is not numeric" in {
          val result = pathBindable.bind("periodKey", "AAAA")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }

        "when period is not letters" in {
          val result = pathBindable.bind("periodKey", "2424")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }

        "when completely invalid format" in {
          val result = pathBindable.bind("periodKey", "invalid")
          result.isLeft mustBe true
          result.left.toOption.get must include("Invalid PeriodKey format")
        }
      }

      "must unbind correctly" in {
        val periodKey = PeriodKey("24AA")
        val result    = pathBindable.unbind("periodKey", periodKey)
        
        result mustBe "24AA"
      }
    }

    "toString" - {
      "must return the value" in {
        val periodKey = PeriodKey("24AA")
        periodKey.toString mustBe "24AA"
      }
    }
  }
}