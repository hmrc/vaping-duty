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

package uk.gov.hmrc.vapingduty.connectors.helpers

import uk.gov.hmrc.vapingduty.utils.{DateTimeHelper, RandomUUIDGenerator}

import java.time.{Clock, Instant}
import javax.inject.Inject

class Headers @Inject()(randomUUIDGenerator: RandomUUIDGenerator, clock: Clock) {

  private val correlationIdHeader: String = "correlationid"
  private val xMessageTypeHeader: String = "X-Message-Type"
  private val xOriginatingSystemHeader: String = "X-Originating-System"
  private val xReceiptDateHeader: String = "X-Receipt-Date"
  private val xRegimeHeader: String = "X-Regime-Type"
  private val xTransmittingSystemHeader: String = "X-Transmitting-System"
  private val X_ZVPD_HEADER = "X-ZVPD"

  private val returnCreateMessage = "VPDReturnCreate"
  private val mdtp = "MDTP"
  private val regime = "VPD"
  private val hip = "HIP"

  // Used when returns branch has been merged
//  def createReturnHeaders(vpdId: VpdId): Seq[(String, String)] =
//    Seq(
//      (correlationIdHeader, randomUUIDGenerator.uuid),
//      (xMessageTypeHeader, returnCreateMessage),
//      (xOriginatingSystemHeader, mdtp),
//      (xReceiptDateHeader, DateTimeHelper.formatISOInstantSeconds(Instant.now(clock))),
//      (xRegimeHeader, regime),
//      (xTransmittingSystemHeader, hip),
//      (X_ZVPD_HEADER, vpdId.toString)
//    )

  private val getObligationsMessage = "GetObligations"

  def createObligationsHeaders: Seq[(String, String)] =
    Seq(
      (correlationIdHeader, randomUUIDGenerator.uuid),
      (xMessageTypeHeader, getObligationsMessage),
      (xOriginatingSystemHeader, mdtp),
      (xReceiptDateHeader, DateTimeHelper.formatISOInstantSeconds(Instant.now(clock))),
      (xRegimeHeader, regime),
      (xTransmittingSystemHeader, hip)
    )
}
