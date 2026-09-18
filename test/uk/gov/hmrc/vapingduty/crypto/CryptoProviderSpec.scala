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

package uk.gov.hmrc.vapingduty.crypto

import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crypto.{Crypted, PlainText}
import uk.gov.hmrc.vapingduty.config.AppConfig

class CryptoProviderSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  private val mockAppConfig = mock[AppConfig]

  "CryptoProvider must" - {

    "when encryption is disabled" - {

      "return NoCrypto" in {
        when(mockAppConfig.cryptoEnabled) thenReturn false
        when(mockAppConfig.cryptoKey) thenReturn "any-key"

        val cryptoProvider = new CryptoProvider(mockAppConfig)
        val crypto = cryptoProvider.getCrypto

        val plainText = PlainText("test data")
        val encrypted = crypto.encrypt(plainText)

        encrypted mustBe Crypted("test data")
      }
    }

    "when encryption is enabled" - {

      "return real crypto that encrypts data" in {
        when(mockAppConfig.cryptoEnabled) thenReturn true
        when(mockAppConfig.cryptoKey) thenReturn "gvBoGdgzqG1AarzF1LY0zQ=="

        val cryptoProvider = new CryptoProvider(mockAppConfig)
        val crypto = cryptoProvider.getCrypto

        val plainText = PlainText("test data")
        val encrypted = crypto.encrypt(plainText)

        encrypted must not be "test data"
        encrypted.value.length must be > 0
      }

      "encrypt and decrypt correctly" in {
        when(mockAppConfig.cryptoEnabled) thenReturn true
        when(mockAppConfig.cryptoKey) thenReturn "gvBoGdgzqG1AarzF1LY0zQ=="

        val cryptoProvider = new CryptoProvider(mockAppConfig)
        val crypto = cryptoProvider.getCrypto

        val original = "sensitive information"
        val encrypted = crypto.encrypt(PlainText(original))
        val decrypted = crypto.decrypt(encrypted)

        decrypted.value mustBe original
      }
    }
  }
}