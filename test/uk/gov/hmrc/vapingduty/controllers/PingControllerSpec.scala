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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mdc.MdcExecutionContext
import uk.gov.hmrc.vapingduty.connectors.VapingDutyStubsConnector

import scala.concurrent.{ExecutionContext, Future}

class PingControllerSpec extends AnyWordSpec
  with Matchers {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = MdcExecutionContext()

  private val fakeRequest = FakeRequest("GET", routes.PingController.ping().url)
  private val vapingDutyStubsConnector = mock[VapingDutyStubsConnector]

  private val controller = PingController(
    vapingDutyStubsConnector,
    Helpers.stubControllerComponents()
  )

  "GET /ping" must {

    "return 200" in {
      when(vapingDutyStubsConnector.ping()(any[HeaderCarrier]()))
        .thenReturn(Future.successful(()))

      val result = controller.ping()(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }
}
