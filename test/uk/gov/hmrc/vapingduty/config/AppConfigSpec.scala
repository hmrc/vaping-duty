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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vapingduty.models.identifiers.{PeriodKey, VpdId}
import uk.gov.hmrc.vapingduty.utils.DateTimeHelper

import java.time.LocalDate

class AppConfigSpec extends AnyFreeSpec with Matchers {

  // Base configuration with all required values
  private val baseConfigMap = Map(
    "appName" -> "vaping-duty",
    "enrolment.serviceName" -> "HMRC-VPD-ORG",
    "enrolment.identifierKey" -> "VPDReference",
    "mongodb.timeToLive" -> "28 days",
    "microservice.services.obligations.protocol" -> "http",
    "microservice.services.obligations.host" -> "localhost",
    "microservice.services.obligations.port" -> 9999,
    "microservice.services.obligations.url" -> "/obligations/vpd",
    "microservice.services.submit-return.protocol" -> "http",
    "microservice.services.submit-return.host" -> "localhost",
    "microservice.services.submit-return.port" -> 8888,
    "microservice.services.submit-return.url.submitReturn" -> "/vpd/return",
    "crypto.enabled" -> true,
    "crypto.key" -> "crypto-key",
    "obligationsMonthsAhead" -> 0
  )

  private def buildAppConfig(overrides: Map[String, Any] = Map.empty): AppConfig = {
    val configMap = baseConfigMap ++ overrides
    val configuration = Configuration.from(configMap)
    val servicesConfig = new ServicesConfig(configuration)
    new AppConfig(configuration, servicesConfig)
  }

  "AppConfig" - {
    "appName" - {
      "must return the configured app name" in {
        val config = buildAppConfig()
        config.appName mustBe "vaping-duty"
      }
    }

    "enrolmentServiceName" - {
      "must return the configured enrolment service name" in {
        val config = buildAppConfig()
        config.enrolmentServiceName mustBe "HMRC-VPD-ORG"
      }
    }

    "enrolmentIdentifierKey" - {
      "must return the configured enrolment identifier key" in {
        val config = buildAppConfig()
        config.enrolmentIdentifierKey mustBe "VPDReference"
      }
    }

    "getObligationsUrl" - {
      val fromDate = DateTimeHelper.formatLocalDate(LocalDate.now())
      val toDate = DateTimeHelper.formatLocalDate(LocalDate.now().minusYears(3))

      "must return the correct URL for obligations" in {
        val config = buildAppConfig()
        val vpdId = VpdId("XMVPD0000000123")
        val result = config.getObligationsUrl(vpdId, fromDate, toDate)

        result must include("http://localhost:9999")
        result must include("/obligations/vpd")
        result must include("displayRequest=A")
        result must include("referenceNumber=XMVPD0000000123")
        result must include("referenceType=VPDReference")
      }

      "must handle different vpdId values" in {
        val config = buildAppConfig()
        val vpdId = VpdId("XMVPD9999999999")
        val result = config.getObligationsUrl(vpdId, fromDate, toDate)

        result must include("referenceNumber=XMVPD9999999999")
      }
    }

    "timeToLive" - {
      "must return the configured time to live in days" in {
        val config = buildAppConfig()
        config.timeToLive mustBe 28
      }

      "must handle different duration formats" in {
        val config = buildAppConfig(Map("mongodb.timeToLive" -> "7 days"))
        config.timeToLive mustBe 7
      }
    }

    "cryptoEnabled" - {
      "must return true when feature is enabled" in {
        val config = buildAppConfig()
        config.cryptoEnabled mustBe true
      }

      "must return false when feature is disabled" in {
        val config = buildAppConfig(Map("crypto.enabled" -> false))
        config.cryptoEnabled mustBe false
      }
    }

    "cryptoKey`" - {
      "must return the crypto key" in {
        val config = buildAppConfig()
        config.cryptoKey mustBe "crypto-key"
      }

      "must handle different API keys" in {
        val config = buildAppConfig(Map("crypto.key" -> "different-key"))
        config.cryptoKey mustBe "different-key"
      }
    }

    "submitReturnUrl" - {
      "must return the correct URL for submit return" in {
        val config = buildAppConfig()
        val result = config.submitReturnUrl()

        result mustBe "http://localhost:8888/vpd/return"
      }

      "must handle different host and port" in {
        val config = buildAppConfig(Map(
          "microservice.services.submit-return.host" -> "submit-host",
          "microservice.services.submit-return.port" -> 9002
        ))
        val result = config.submitReturnUrl()

        result mustBe "http://submit-host:9002/vpd/return"
      }
    }

    "getReturnUrl" - {
      "must return the correct URL for get return" in {
        val config = buildAppConfig()
        val vpdId = VpdId("XMVPD0000000123")
        val periodKey = PeriodKey("24AA")
        val result = config.getReturnUrl(vpdId, periodKey)

        result mustBe "http://localhost:8888/vpd/return/XMVPD0000000123/24AA"
      }

      "must handle different vpdId and periodKey values" in {
        val config = buildAppConfig()
        val vpdId = VpdId("XMVPD9999999999")
        val periodKey = PeriodKey("24AB")
        val result = config.getReturnUrl(vpdId, periodKey)

        result mustBe "http://localhost:8888/vpd/return/XMVPD9999999999/24AB"
      }
    }

    "getConfStringAndThrowIfNotFound" - {
      "must throw RuntimeException when key is not found" in {
        val config = buildAppConfig()

        val exception = intercept[RuntimeException] {
          config.getConfStringAndThrowIfNotFound("submit-return.url.nonExistent")
        }

        exception.getMessage must include("Could not find services config key")
        exception.getMessage must include("submit-return.url.nonExistent")
      }
    }

    "integration tests" - {
      "must handle complete configuration with all values" in {
        val config = buildAppConfig()

        config.appName mustBe "vaping-duty"
        config.enrolmentServiceName mustBe "HMRC-VPD-ORG"
        config.enrolmentIdentifierKey mustBe "VPDReference"
        config.timeToLive mustBe 28
      }
    }
  }
}