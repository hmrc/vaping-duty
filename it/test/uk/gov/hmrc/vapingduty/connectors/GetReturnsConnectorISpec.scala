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
import uk.gov.hmrc.vapingduty.models.obligations.ObligationsResponse
import uk.gov.hmrc.vapingduty.models.returns.VapingProductsProduced
import uk.gov.hmrc.vapingduty.utils.{ConnectorTestHelpers, WireMockHelper}

class GetReturnsConnectorISpec extends ISpecBase with WireMockHelper with ConnectorTestHelpers {
  protected val endpointName = "getReturns"

  "GetReturnsConnector when" - {
    "getReturns is called must" - {
      "successfully get returns" in new SetUp {
        stubGet(
          url,
          OK,
          Json.toJson(VapingProductsProduced(Seq.empty, Seq.empty)).toString()
        )
        whenReady(connector.getReturn(periodKey, vpdId)) { result =>
          result mustBe ObligationsResponse(Seq.empty)
          verifyGet(url)
        }
      }

//      "fail with InternalServerException if the call returns an invalid response json" in new SetUp {
//        stubGet(url, OK, Json.toJson("invalid").toString)
//
//        val result = connector.getReturn(periodKey, vpdId)
//
//        whenReady(result.failed) { exception =>
//          assertExceptionMessage(exception, "Unable to parse returns response")
//          verifyGet(url)
//        }
//      }
//
//      "fail with InternalServerException if the call returns a 400 response" in new SetUp {
//        stubGet(
//          url,
//          BAD_REQUEST,
//          Json.toJson(ObligationsResponse(Seq.empty)).toString(),
//        )
//
//        val result = connector.getObligations(vpdId)
//
//        whenReady(result.failed) { exception =>
//          assertExceptionMessage(exception, "Failed to get obligations")
//          verifyGet(url)
//        }
//      }
//
//      "fail with InternalServerException if the call returns a 422 response" in new SetUp {
//        stubGet(
//          url,
//          UNPROCESSABLE_ENTITY,
//          Json.toJson(ObligationsResponse(Seq.empty)).toString()
//        )
//
//        val result = connector.getObligations(vpdId)
//
//        whenReady(result.failed) { exception =>
//          assertExceptionMessage(exception, "Failed to get obligations")
//          verifyGet(url)
//        }
//      }
//
//      "fail with InternalServerException if the call returns a 500 response" in new SetUp {
//        stubGet(
//          url,
//          INTERNAL_SERVER_ERROR,
//          Json.toJson(ObligationsResponse(Seq.empty)).toString()
//        )
//
//        val result = connector.getObligations(vpdId)
//
//        whenReady(result.failed) { exception =>
//          assertExceptionMessage(exception, "Failed to get obligations")
//          verifyGet(url)
//        }
//      }
//
//      "fail with InternalServerException when a network fault occurs" in new SetUp {
//        stubGetFault(
//          url,
//          Fault.EMPTY_RESPONSE
//        )
//
//        val result = connector.getObligations(vpdId)
//
//        whenReady(result.failed) { exception =>
//          assertExceptionMessage(exception, "Failed to get obligations")
//          verifyGet(url)
//        }
//      }
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
    val connector       = app.injector.instanceOf[GetReturnsConnector]
    val url             = config.getObligationsUrl(vpdId)
  }
}
