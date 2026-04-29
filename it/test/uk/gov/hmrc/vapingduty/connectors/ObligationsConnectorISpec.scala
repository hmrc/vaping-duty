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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlMatching}
import org.scalatest.freespec.AnyFreeSpec
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.vapingduty.base.ISpecBase
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.obligations.ObligationsResponse
import uk.gov.hmrc.vapingduty.utils.WireMockHelper

class ObligationsConnectorISpec extends ISpecBase with WireMockHelper {

  override def fakeApplication(): Application = {
    applicationBuilder().configure("microservice.services.obligations.port" -> server.port()).build()
  }

  val vpdId = VpdId(id = "vpdId")

  "ObligationsConnector" - {
    "get" - {
      "must successfully ping the vaping duty stubs service" in new Setup {
        server.stubFor(
          get(urlMatching(url))
            .willReturn(
              aResponse().withBody(Json.toJson(ObligationsResponse(Seq.empty)).toString)
            )
        )

        whenReady(connector.getObligations(vpdId)) { result =>
          result mustBe ObligationsResponse(Seq.empty)
        }
      }

      "must fail when authorisation fails" in new Setup {
        server.stubFor(
          get(urlMatching(url))
            .willReturn(
              aResponse()
                .withStatus(UNAUTHORIZED)
            )
        )

        whenReady(connector.getObligations(vpdId).failed) { e =>
          e.getMessage must include("Failed to get obligations")
        }
      }

      "must fail when an unexpected status code is returned" in new Setup {
        server.stubFor(
          get(urlMatching(url))
            .willReturn(
              aResponse()
                .withStatus(CREATED)
            )
        )

        whenReady(connector.getObligations(vpdId).failed) { e =>
          e.getMessage must include("Failed to get obligations")
        }
      }

      "must fail with an Exception when an internal server error status code is returned" in new Setup {
        server.stubFor(
          get(urlMatching(url))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withStatusMessage("<test error message>")
            )
        )

        whenReady(connector.getObligations(vpdId).failed) { e =>
          e.getMessage must include("Failed to get obligations")
        }
      }
    }
  }

  class Setup {
    val connector = app.injector.instanceOf[ObligationsConnector]
    val url = s"/etmp/RESTAdapter/cross-regime/taxpayer-obligations\\?displayRequest=A&referenceNumber=vpdId&referenceType=ZVPD"
  }
}
