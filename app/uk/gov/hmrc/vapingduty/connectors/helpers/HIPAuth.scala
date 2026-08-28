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

package uk.gov.hmrc.vapingduty.connectors.helpers

import uk.gov.hmrc.vapingduty.config.AppConfig

import java.util.Base64
import javax.inject.Inject

class HIPAuth @Inject()(appConfig: AppConfig) {

  def authorizationForReturns(): String =
    createBasicAuth(appConfig.returnsClientId, appConfig.returnsSecret)

  def authorizationForObligations(): String =
    createBasicAuth(appConfig.obligationsClientId, appConfig.obligationsSecret)

  private def createBasicAuth(clientId: String, secret: String): String = {
    val encoded = Base64.getEncoder.encodeToString(s"$clientId:$secret".getBytes("UTF-8"))
    s"Basic $encoded"
  }
}

