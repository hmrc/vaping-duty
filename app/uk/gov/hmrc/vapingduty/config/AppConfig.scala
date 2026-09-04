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

  def timeToLive: Long = Duration(config.get[String]("mongodb.timeToLive")).toDays.toInt

  // Obligations
  private val obligationsHost: String = servicesConfig.baseUrl("obligations")
  private val obligationsUrl: String = config.get[String]("microservice.services.obligations.url")
  private val allObligations = "A"

  private def obligationsQueryString(vpdId: VpdId) =
    s"?displayRequest=$allObligations&referenceNumber=$vpdId&referenceType=$enrolmentIdentifierKey"

  def getObligationsUrl(vpdId: VpdId): String = s"$obligationsHost$obligationsUrl${obligationsQueryString(vpdId)}"

  def obligationsClientId: String = config.get[String]("microservice.services.obligations.clientId")

  def obligationsSecret: String = config.get[String]("microservice.services.obligations.secret")

  // Returns
  private val vpdReturnHost: String = servicesConfig.baseUrl("submit-return")
  private lazy val vpdReturnUrlPrefix = getConfStringAndThrowIfNotFound("submit-return.url.submitReturn")

  def submitReturnUrl(): String = s"$vpdReturnHost$vpdReturnUrlPrefix"

  def getReturnUrl(vpdReference: VpdId, periodKey: PeriodKey): String = s"$vpdReturnHost$vpdReturnUrlPrefix/$vpdReference/$periodKey"

  def returnsClientId: String = config.get[String]("microservice.services.submit-return.clientId")

  def returnsSecret: String = config.get[String]("microservice.services.submit-return.secret")

  // NRS
  private val nrsBaseUrl: String = servicesConfig.baseUrl("nrs")
  private val nrsUrl: String = config.get[String]("microservice.services.nrs.url")

  def nrsSubmissionUrl: String = s"$nrsBaseUrl$nrsUrl"

  def nrsApiKey: String = config.get[String]("microservice.services.nrs.api-key")

  def nrsWorkItemTTL: Int = Duration(config.get[String]("mongodb.nrs-work-item.ttl")).toDays.toInt

  def nrsWorkItemRetryAfter: scala.concurrent.duration.FiniteDuration =
    config.get[scala.concurrent.duration.FiniteDuration]("mongodb.nrs-work-item.retry-after")

  def nrsWorkItemMaxRetries: Int = config.get[Int]("mongodb.nrs-work-item.max-retries")

  def nrsWorkItemExponentialBackoffFactor: Double = config.get[Double]("mongodb.nrs-work-item.exponential-backoff-factor")

  val nrsSubmissionEnabled: Boolean = config.get[Boolean]("features.nrs-submission-enabled")
  val nrsGenerationEnabled: Boolean = config.get[Boolean]("features.nrs-generation-enabled")
}
