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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption

import java.time.{Instant, Month}

final case class UserAnswers(
                              vpdId: String,
                              periodKey: String,
                              returnPeriod: Option[Month] = None,
                              year: Option[String] = None,
                              data: JsObject = Json.obj(),
                              startedTime: Instant,
                              lastUpdated: Instant
)

object UserAnswers {

  private implicit val monthFormat: Format[Month] = implicitly[Format[String]].inmap(Month.valueOf, _.toString)

  // HTTP format - used by controllers for request/response JSON (no encryption)
  implicit val httpFormat: OFormat[UserAnswers] = {
    implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
    Json.format[UserAnswers]
  }

  // MongoDB format - used by repository for database storage (with encryption)
  def mongoFormat(implicit crypto: Encrypter with Decrypter): OFormat[UserAnswers] = {
    implicit val sensitiveFormat: Format[SensitiveString] = sensitiveStringFormat
    
    (
      (__ \ "vpdId").format[String] and
      (__ \ "periodKey").format[String] and
      (__ \ "returnPeriod").formatNullable[Month] and
      (__ \ "year").formatNullable[String] and
      (__ \ "data").format[SensitiveString]
        .inmap[JsObject](
          s => Json.parse(s.decryptedValue).as[JsObject],
          js => SensitiveString(Json.stringify(js))
        ) and
      (__ \ "startedTime").format(MongoJavatimeFormats.instantFormat) and
      (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat)
    )(UserAnswers.apply, o => Tuple.fromProductTyped(o))
  }

  private def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
}
