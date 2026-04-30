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

package uk.gov.hmrc.vapingduty.connectors

import com.github.tomakehurst.wiremock.http.Fault
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingduty.base.ISpecBase
import uk.gov.hmrc.vapingduty.utils.{ConnectorTestHelpers, WireMockHelper}

class ReturnsConnectorISpec extends ISpecBase with WireMockHelper with ConnectorTestHelpers {
  protected val endpointName = "submit-return"

  "ReturnsConnector when" - {
    "submitReturn is called must" - {
      "successfully submit a return" in new SetUp {
        stubPost(
          submitReturnUrl,
          OK,
          Json.toJson(returnsCreateRequest).toString(),
          Json.toJson(returnCreateResponseSuccess).toString()
        )
        whenReady(connector.submitReturn(returnsCreateRequest, vpdId)) { result =>
          result mustBe returnCreateResponseSuccess.success
          verifyPost(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns an invalid response json" in new SetUp {
        stubPost(submitReturnUrl, OK, Json.toJson(returnsCreateRequest).toString(), "invalid")

        val result = connector.submitReturn(returnsCreateRequest, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Parsing failed for VPD return submission response")
          verifyPost(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 400 response" in new SetUp {
        stubPost(
          submitReturnUrl,
          BAD_REQUEST,
          Json.toJson(returnsCreateRequest).toString(),
          ""
        )

        val result = connector.submitReturn(returnsCreateRequest, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit VPD return")
          verifyPost(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 422 response" in new SetUp {
        stubPost(
          submitReturnUrl,
          UNPROCESSABLE_ENTITY,
          Json.toJson(returnsCreateRequest).toString(),
          ""
        )

        val result = connector.submitReturn(returnsCreateRequest, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit VPD return")
          verifyPost(submitReturnUrl)
        }
      }

      "fail with InternalServerException if the call returns a 500 response" in new SetUp {
        stubPost(
          submitReturnUrl,
          INTERNAL_SERVER_ERROR,
          Json.toJson(returnsCreateRequest).toString(),
          ""
        )

        val result = connector.submitReturn(returnsCreateRequest, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit VPD return")
          verifyPost(submitReturnUrl)
        }
      }

      "fail with InternalServerException when a network fault occurs" in new SetUp {
        stubPostFault(
          submitReturnUrl,
          Json.toJson(returnsCreateRequest).toString(),
          Fault.EMPTY_RESPONSE
        )

        val result = connector.submitReturn(returnsCreateRequest, vpdId)

        whenReady(result.failed) { exception =>
          assertExceptionMessage(exception, "Failed to submit VPD return")
          verifyPost(submitReturnUrl)
        }
      }
    }
  }

  private def assertExceptionMessage(exception: Throwable, expectedMessage: String) = {
    exception match {
      case ex: InternalServerException =>
        ex.getMessage must include(expectedMessage)
      case _ =>
        fail(s"Expected an InternalServerException but got ${exception.getClass.getSimpleName}")
    }
  }

  abstract class SetUp extends ConnectorFixture {
    val connector       = appWithHttpClientV2.injector.instanceOf[ReturnsConnector]
    val submitReturnUrl = config.submitReturnUrl()
  }
}
