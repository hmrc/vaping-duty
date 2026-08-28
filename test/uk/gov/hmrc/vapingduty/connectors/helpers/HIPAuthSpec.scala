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

import org.mockito.Mockito.when
import uk.gov.hmrc.vapingduty.base.SpecBase
import uk.gov.hmrc.vapingduty.config.AppConfig

import java.util.Base64

class HIPAuthSpec extends SpecBase {

  private val TEST_RETURNS_CLIENT_ID     = "test-returns-client-id"
  private val TEST_RETURNS_SECRET        = "test-returns-secret"
  private val TEST_OBLIGATIONS_CLIENT_ID = "test-obligations-client-id"
  private val TEST_OBLIGATIONS_SECRET    = "test-obligations-secret"
  private val BASIC_PREFIX               = "Basic "

  val mockAppConfig: AppConfig = mock[AppConfig]
  val hipAuth                  = new HIPAuth(mockAppConfig)

  "authorizationForReturns must" - {
    "return a correctly formatted Basic auth string" in {
      when(mockAppConfig.returnsClientId).thenReturn(TEST_RETURNS_CLIENT_ID)
      when(mockAppConfig.returnsSecret).thenReturn(TEST_RETURNS_SECRET)

      val result = hipAuth.authorizationForReturns()

      result must startWith(BASIC_PREFIX)
    }

    "encode credentials in the correct format" in {
      when(mockAppConfig.returnsClientId).thenReturn(TEST_RETURNS_CLIENT_ID)
      when(mockAppConfig.returnsSecret).thenReturn(TEST_RETURNS_SECRET)

      val result        = hipAuth.authorizationForReturns()
      val encodedPart   = result.stripPrefix(BASIC_PREFIX)
      val decodedBytes  = Base64.getDecoder.decode(encodedPart)
      val decodedString = new String(decodedBytes, "UTF-8")

      decodedString mustBe s"$TEST_RETURNS_CLIENT_ID:$TEST_RETURNS_SECRET"
    }

    "use credentials from AppConfig" in {
      when(mockAppConfig.returnsClientId).thenReturn(TEST_RETURNS_CLIENT_ID)
      when(mockAppConfig.returnsSecret).thenReturn(TEST_RETURNS_SECRET)

      val expectedCredentials = s"$TEST_RETURNS_CLIENT_ID:$TEST_RETURNS_SECRET"
      val expectedEncoded     = Base64.getEncoder.encodeToString(expectedCredentials.getBytes("UTF-8"))
      val expectedResult      = s"$BASIC_PREFIX$expectedEncoded"

      val result = hipAuth.authorizationForReturns()

      result mustBe expectedResult
    }
  }

  "authorizationForObligations must" - {
    "return a correctly formatted Basic auth string" in {
      when(mockAppConfig.obligationsClientId).thenReturn(TEST_OBLIGATIONS_CLIENT_ID)
      when(mockAppConfig.obligationsSecret).thenReturn(TEST_OBLIGATIONS_SECRET)

      val result = hipAuth.authorizationForObligations()

      result must startWith(BASIC_PREFIX)
    }

    "encode credentials in the correct format" in {
      when(mockAppConfig.obligationsClientId).thenReturn(TEST_OBLIGATIONS_CLIENT_ID)
      when(mockAppConfig.obligationsSecret).thenReturn(TEST_OBLIGATIONS_SECRET)

      val result        = hipAuth.authorizationForObligations()
      val encodedPart   = result.stripPrefix(BASIC_PREFIX)
      val decodedBytes  = Base64.getDecoder.decode(encodedPart)
      val decodedString = new String(decodedBytes, "UTF-8")

      decodedString mustBe s"$TEST_OBLIGATIONS_CLIENT_ID:$TEST_OBLIGATIONS_SECRET"
    }

    "use credentials from AppConfig" in {
      when(mockAppConfig.obligationsClientId).thenReturn(TEST_OBLIGATIONS_CLIENT_ID)
      when(mockAppConfig.obligationsSecret).thenReturn(TEST_OBLIGATIONS_SECRET)

      val expectedCredentials = s"$TEST_OBLIGATIONS_CLIENT_ID:$TEST_OBLIGATIONS_SECRET"
      val expectedEncoded     = Base64.getEncoder.encodeToString(expectedCredentials.getBytes("UTF-8"))
      val expectedResult      = s"$BASIC_PREFIX$expectedEncoded"

      val result = hipAuth.authorizationForObligations()

      result mustBe expectedResult
    }
  }
}
