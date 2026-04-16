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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.PathBindable
import uk.gov.hmrc.vapingduty.utils.Constants.*

case class CredentialId(id: String) {
  override def toString: String = id
}

object CredentialId {
  given PathBindable[CredentialId] = new PathBindable[CredentialId] {

    override def bind(key: String, value: String): Either[String, CredentialId] = {
      validate(value)
    }

    override def unbind(key: String, value: CredentialId): String = value.toString
  }

  private def validate(value: String) =
    if (value.length > credentialIdMaxLength) {
      Left("Invalid CredentialId")
    } else {
      Right(CredentialId(value))
    }

  given OFormat[CredentialId] = Json.format[CredentialId]
}
