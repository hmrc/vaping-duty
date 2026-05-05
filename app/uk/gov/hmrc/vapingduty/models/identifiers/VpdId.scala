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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.PathBindable

case class VpdId(id: String) {
  override def toString: String = id
}

object VpdId {
  private val vpdIdPattern = "^(?:GB|XI)WK[0-9]{7}WK$".r

  given PathBindable[VpdId] = new PathBindable[VpdId] {

    override def bind(key: String, value: String): Either[String, VpdId] = {
      validate(value)
    }

    override def unbind(key: String, value: VpdId): String = value.toString
  }

  private def validate(value: String) =
    if (vpdIdPattern.matches(value)) {
      Right(VpdId(value))
    } else {
      Left("Invalid VpdId")
    }

  given OFormat[VpdId] = Json.format[VpdId]
}
