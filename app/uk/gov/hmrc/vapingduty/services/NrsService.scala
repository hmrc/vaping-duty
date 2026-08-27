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

package uk.gov.hmrc.vapingduty.services

import play.api.Logging
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.vapingduty.connectors.NrsConnector
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsMetadata, NrsPayload}
import uk.gov.hmrc.vapingduty.utils.{DateTimeService, NrsUtils}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject() (
  nrsConnector: NrsConnector,
  nrsUtils: NrsUtils,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends Logging {

  def submitToNrs(
    payload: JsValue,
    identityData: IdentityData,
    notableEvent: String
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    val encodedPayload = nrsUtils.encode(payload)
    val checksum       = nrsUtils.sha256Hash(payload.toString)
    val timestamp      = dateTimeService.timestamp()

    val metadata = NrsMetadata(
      businessId = "vpd",
      notableEvent = notableEvent,
      payloadContentType = "application/json",
      payloadSha256Checksum = checksum,
      userSubmissionTimestamp = timestamp,
      identityData = identityData,
      userAuthToken = hc.authorization.map(_.value).getOrElse(""),
      headerData = nrsUtils.buildHeaderData(hc),
      searchKeys = nrsUtils.buildSearchKeys(identityData)
    )

    val nrsPayload = NrsPayload(
      payload = encodedPayload,
      metadata = metadata
    )

    nrsConnector.submitToNrs(nrsPayload).map {
      case Right(_)    =>
        logger.info(s"Successfully submitted to NRS for notable event: $notableEvent")
        Right(())
      case Left(error) =>
        logger.error(s"Failed to submit to NRS for notable event: $notableEvent - ${error.getMessage}")
        Left(error)
    }
  }
}