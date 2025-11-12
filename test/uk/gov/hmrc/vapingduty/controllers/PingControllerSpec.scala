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

package uk.gov.hmrc.vapingduty.controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.vapingduty.connectors.VapingDutyStubsConnector
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.mdc.MdcExecutionContext
import play.api.inject.bind

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.ArgumentMatchers.any
import scala.concurrent.Future
import play.api.mvc.Results.Ok

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlMatching}
import org.scalatest.freespec.AnyFreeSpec
import play.api.Application
import play.api.http.Status.*
import uk.gov.hmrc.vapingduty.connectors.VapingDutyStubsConnectorHttp
import play.inject.guice.GuiceApplicationBuilder
import org.scalatestplus.mockito.MockitoSugar.mock

class PingControllerSpec extends AnyWordSpec
     with Matchers {


  private val fakeRequest              = FakeRequest("GET", "/ping")
  implicit val ec: ExecutionContext    = MdcExecutionContext()
  private val vapingDutyStubsConnector = mock[VapingDutyStubsConnector]
  private val controller               = new PingController(
                                          vapingDutyStubsConnector,
                                          Helpers.stubControllerComponents(),
                                          HeaderCarrier()
                                        )


    "GET /ping" must {
      "return 200" in {
        val application = new GuiceApplicationBuilder()
                            .overrides(
                              bind[VapingDutyStubsConnector].toInstance(vapingDutyStubsConnector),
                            )
                            .build()
        running(application) {
        when(vapingDutyStubsConnector.ping()(any()))
          .thenReturn(Future.successful(
            Ok("ping")
          ))

        val result = controller.ping()(fakeRequest)
        status(result) shouldBe Status.OK
        }
      }
    }
}
