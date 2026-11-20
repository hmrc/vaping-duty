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

package uk.gov.hmrc.vapingduty.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.connectors.ObligationsConnector
import uk.gov.hmrc.vapingduty.models.identifiers.VpdId
import uk.gov.hmrc.vapingduty.models.obligations.{ObligationDetails, ObligationItem}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ObligationsService @Inject()(
                                    config: AppConfig,
                                    obligationsConnector: ObligationsConnector
                                  )(using ExecutionContext) {

  // Returns the obligations for the VPD identification only, flattened into a single list
  def getObligations(vpdId: VpdId)(using HeaderCarrier): Future[Seq[ObligationDetails]] =
    obligationsConnector.getObligations(vpdId).map(
      _.obligation
        .filter(matchesVpdId(vpdId))
        .flatMap(_.obligationDetails)
    )

  private def matchesVpdId(vpdId: VpdId)(obligation: ObligationItem): Boolean =
    obligation.identification.exists { id =>
      id.referenceType == config.enrolmentIdentifierKey &&
        id.referenceNumber == vpdId.id
    }
}
