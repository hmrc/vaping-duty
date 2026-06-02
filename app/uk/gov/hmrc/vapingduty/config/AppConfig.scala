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

package uk.gov.hmrc.vapingduty.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vapingduty.models.identifiers.{PeriodKey, VpdId}

import scala.concurrent.duration.Duration

@SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
@Singleton
class AppConfig @Inject()(
          config: Configuration,
          servicesConfig: ServicesConfig
  ) {

  private[config] def getConfStringAndThrowIfNotFound(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"Could not find services config key '$key'"))

  val appName: String = config.get[String]("appName")
  val enrolmentServiceName: String = config.get[String]("enrolment.serviceName")
  val enrolmentIdentifierKey: String = config.get[String]("enrolment.identifierKey")

  private val obligationsHost: String = servicesConfig.baseUrl("obligations")
  private val obligationsUrl: String = config.get[String]("microservice.services.obligations.url")
  private val allObligations = "A"
  private def obligationsQueryString(vpdId: VpdId) =
    s"?displayRequest=$allObligations&referenceNumber=$vpdId&referenceType=$enrolmentIdentifierKey"

  def getObligationsUrl(vpdId: VpdId): String = s"$obligationsHost$obligationsUrl${obligationsQueryString(vpdId)}"

  def timeToLive: Long = Duration(config.get[String]("mongodb.timeToLive")).toDays.toInt

  private val vpdReturnHost: String = servicesConfig.baseUrl("submit-return")
  private lazy val vpdReturnUrlPrefix = getConfStringAndThrowIfNotFound("submit-return.url.submitReturn")

  def submitReturnUrl(): String = s"$vpdReturnHost$vpdReturnUrlPrefix"
  def getReturnUrl(vpdReference: VpdId, periodKey: PeriodKey): String = s"$vpdReturnHost$vpdReturnUrlPrefix/$vpdReference/$periodKey"
}
