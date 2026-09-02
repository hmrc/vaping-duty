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

import scala.concurrent.duration._

class AppConfigSpec extends AnyFreeSpec with Matchers {

  // Base configuration with all required values
  private val baseConfigMap = Map(
    "appName" -> "vaping-duty",
    "enrolment.serviceName" -> "HMRC-VPD-ORG",
    "enrolment.identifierKey" -> "VPDReference",
    "mongodb.timeToLive" -> "28 days",
    "mongodb.nrs-work-item.ttl" -> "7 days",
    "mongodb.nrs-work-item.retry-after" -> "1 hour",
    "mongodb.nrs-work-item.max-retries" -> 3,
    "mongodb.nrs-work-item.exponential-backoff-factor" -> 2.0,
    "microservice.services.obligations.protocol" -> "http",
    "microservice.services.obligations.host" -> "localhost",
    "microservice.services.obligations.port" -> 9999,
    "microservice.services.obligations.url" -> "/obligations/vpd",
    "microservice.services.submit-return.protocol" -> "http",
    "microservice.services.submit-return.host" -> "localhost",
    "microservice.services.submit-return.port" -> 8888,
    "microservice.services.submit-return.url.submitReturn" -> "/vpd/return",
    "microservice.services.nrs.protocol" -> "https",
    "microservice.services.nrs.host" -> "nrs.service",
    "microservice.services.nrs.port" -> 443,
    "microservice.services.nrs.url" -> "/submission",
    "microservice.services.nrs.api-key" -> "test-api-key-12345",
    "microservice.services.nrs.circuit-breaker.max-failures" -> 5,
    "microservice.services.nrs.circuit-breaker.call-timeout" -> "30 seconds",
    "microservice.services.nrs.circuit-breaker.reset-timeout" -> "60 seconds",
    "microservice.services.nrs.nrs-throttle-duration" -> "5 seconds",
    "microservice.services.nrs.lock-service-ttl" -> "10 minutes"
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
      "must return the correct URL for obligations" in {
        val config = buildAppConfig()
        val vpdId = VpdId("XMVPD0000000123")
        val result = config.getObligationsUrl(vpdId)

        result must include("http://localhost:9999")
        result must include("/obligations/vpd")
        result must include("displayRequest=A")
        result must include("referenceNumber=XMVPD0000000123")
        result must include("referenceType=VPDReference")
      }

      "must handle different vpdId values" in {
        val config = buildAppConfig()
        val vpdId = VpdId("XMVPD9999999999")
        val result = config.getObligationsUrl(vpdId)

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

    "nrsSubmissionUrl" - {
      "must return the correct URL for NRS submission" in {
        val config = buildAppConfig()
        config.nrsSubmissionUrl mustBe "https://nrs.service:443/submission"
      }

      "must handle different NRS configuration" in {
        val config = buildAppConfig(Map(
          "microservice.services.nrs.host" -> "nrs-prod.service",
          "microservice.services.nrs.port" -> 8443,
          "microservice.services.nrs.url" -> "/nrs/submission"
        ))
        config.nrsSubmissionUrl mustBe "https://nrs-prod.service:8443/nrs/submission"
      }
    }

    "nrsApiKey" - {
      "must return the NRS API key" in {
        val config = buildAppConfig()
        config.nrsApiKey mustBe "test-api-key-12345"
      }

      "must handle different API keys" in {
        val config = buildAppConfig(Map(
          "microservice.services.nrs.api-key" -> "different-key"
        ))
        config.nrsApiKey mustBe "different-key"
      }
    }

    "nrsCircuitBreakerMaxFailures" - {
      "must return the configured max failures" in {
        val config = buildAppConfig()
        config.nrsCircuitBreakerMaxFailures mustBe 5
      }

      "must handle different max failures values" in {
        val config = buildAppConfig(Map(
          "microservice.services.nrs.circuit-breaker.max-failures" -> 10
        ))
        config.nrsCircuitBreakerMaxFailures mustBe 10
      }
    }

    "nrsCircuitBreakerCallTimeout" - {
      "must return the configured call timeout" in {
        val config = buildAppConfig()
        config.nrsCircuitBreakerCallTimeout mustBe 30.seconds
      }

      "must handle different timeout values" in {
        val config = buildAppConfig(Map(
          "microservice.services.nrs.circuit-breaker.call-timeout" -> "60 seconds"
        ))
        config.nrsCircuitBreakerCallTimeout mustBe 60.seconds
      }
    }

    "nrsCircuitBreakerResetTimeout" - {
      "must return the configured reset timeout" in {
        val config = buildAppConfig()
        config.nrsCircuitBreakerResetTimeout mustBe 60.seconds
      }

      "must handle different reset timeout values" in {
        val config = buildAppConfig(Map(
          "microservice.services.nrs.circuit-breaker.reset-timeout" -> "120 seconds"
        ))
        config.nrsCircuitBreakerResetTimeout mustBe 120.seconds
      }
    }

    "nrsThrottleDuration" - {
      "must return the configured throttle duration" in {
        val config = buildAppConfig()
        config.nrsThrottleDuration mustBe 5.seconds
      }

      "must handle different throttle durations" in {
        val config = buildAppConfig(Map(
          "microservice.services.nrs.nrs-throttle-duration" -> "10 seconds"
        ))
        config.nrsThrottleDuration mustBe 10.seconds
      }
    }

    "nrsLockServiceTTL" - {
      "must return the configured lock service TTL" in {
        val config = buildAppConfig()
        config.nrsLockServiceTTL mustBe 10.minutes
      }

      "must handle different TTL values" in {
        val config = buildAppConfig(Map(
          "microservice.services.nrs.lock-service-ttl" -> "5 minutes"
        ))
        config.nrsLockServiceTTL mustBe 5.minutes
      }
    }

    "nrsWorkItemTTL" - {
      "must return the configured work item TTL in days" in {
        val config = buildAppConfig()
        config.nrsWorkItemTTL mustBe 7
      }

      "must handle different TTL values" in {
        val config = buildAppConfig(Map(
          "mongodb.nrs-work-item.ttl" -> "14 days"
        ))
        config.nrsWorkItemTTL mustBe 14
      }
    }

    "nrsWorkItemRetryAfter" - {
      "must return the configured retry after duration" in {
        val config = buildAppConfig()
        config.nrsWorkItemRetryAfter mustBe 1.hour
      }

      "must handle different retry durations" in {
        val config = buildAppConfig(Map(
          "mongodb.nrs-work-item.retry-after" -> "30 minutes"
        ))
        config.nrsWorkItemRetryAfter mustBe 30.minutes
      }
    }

    "nrsWorkItemMaxRetries" - {
      "must return the configured max retries" in {
        val config = buildAppConfig()
        config.nrsWorkItemMaxRetries mustBe 3
      }

      "must handle zero retries" in {
        val config = buildAppConfig(Map(
          "mongodb.nrs-work-item.max-retries" -> 0
        ))
        config.nrsWorkItemMaxRetries mustBe 0
      }

      "must handle higher retry counts" in {
        val config = buildAppConfig(Map(
          "mongodb.nrs-work-item.max-retries" -> 10
        ))
        config.nrsWorkItemMaxRetries mustBe 10
      }
    }

    "nrsWorkItemExponentialBackoffFactor" - {
      "must return the configured exponential backoff factor" in {
        val config = buildAppConfig()
        config.nrsWorkItemExponentialBackoffFactor mustBe 2.0
      }

      "must handle different factors" in {
        val config = buildAppConfig(Map(
          "mongodb.nrs-work-item.exponential-backoff-factor" -> 1.5
        ))
        config.nrsWorkItemExponentialBackoffFactor mustBe 1.5
      }

      "must handle factor of 3.0" in {
        val config = buildAppConfig(Map(
          "mongodb.nrs-work-item.exponential-backoff-factor" -> 3.0
        ))
        config.nrsWorkItemExponentialBackoffFactor mustBe 3.0
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
        config.nrsWorkItemTTL mustBe 7
        config.nrsWorkItemMaxRetries mustBe 3
        config.nrsApiKey mustBe "test-api-key-12345"
        config.nrsCircuitBreakerMaxFailures mustBe 5
        config.nrsCircuitBreakerCallTimeout mustBe 30.seconds
        config.nrsCircuitBreakerResetTimeout mustBe 60.seconds
        config.nrsThrottleDuration mustBe 5.seconds
        config.nrsLockServiceTTL mustBe 10.minutes
        config.nrsWorkItemRetryAfter mustBe 1.hour
        config.nrsWorkItemExponentialBackoffFactor mustBe 2.0
      }
    }
  }
}