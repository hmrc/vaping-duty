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

import org.mongodb.scala.bson.ObjectId
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.vapingduty.connectors.NrsConnector
import uk.gov.hmrc.vapingduty.models.nrs.{IdentityData, NrsMetadata, NrsPayload, NrsSubmissionWorkItem}
import uk.gov.hmrc.vapingduty.repositories.NrsWorkItemRepository
import uk.gov.hmrc.vapingduty.utils.{DateTimeService, NrsUtils}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject() (
  nrsConnector: NrsConnector,
  nrsUtils: NrsUtils,
  dateTimeService: DateTimeService,
  nrsWorkItemRepository: NrsWorkItemRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  /**
   * Queue a work item for NRS submission. This is the method that should be called
   * from controllers after successful ETMP submission.
   *
   * @param payload The JSON payload to submit to NRS
   * @param identityData User identity data from auth
   * @param notableEvent The notable event identifier
   * @param hc HeaderCarrier for HTTP context
   * @return Future[Unit] - fire and forget, failures are logged
   */
  def makeWorkItemAndQueue(
    payload: JsValue,
    identityData: IdentityData,
    notableEvent: String
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val payloadString = Json.stringify(payload)
    val checksum = nrsUtils.sha256Hash(payloadString)
    val timestamp = dateTimeService.timestamp()
    val userAuthToken = hc.authorization.map(_.value).getOrElse("")
    val headerData = hc.headers(Seq("User-Agent", "X-Request-ID", "X-Session-ID")).toMap
    val vpdId = identityData.internalId.getOrElse("")

    val metadata = NrsMetadata.create(
      payLoad = payloadString,
      sha256Hash = checksum,
      identityData = identityData,
      submissionTimeStamp = timestamp,
      userAuthToken = userAuthToken,
      userHeaderData = headerData,
      vpdId = vpdId
    )

    val encodedPayload = nrsUtils.encode(payloadString)
    val nrsPayload = NrsPayload(
      payload = encodedPayload,
      metadata = metadata
    )

    val workItem = NrsSubmissionWorkItem(
      nrsPayload = nrsPayload
    )

    nrsWorkItemRepository
      .pushNew(workItem, Instant.now(), ProcessingStatus.ToDo)
      .map { _ =>
        logger.info(s"Successfully queued NRS work item for notable event: $notableEvent")
        ()
      }
      .recover { case ex =>
        logger.warn(s"Failed to queue NRS work item for notable event: $notableEvent", ex)
        ()
      }
  }

  /**
   * Submit directly to NRS (used by the scheduled service to process work items).
   * Controllers should NOT call this method directly - use makeWorkItemAndQueue instead.
   *
   * @param payload The NRS payload to submit
   * @param hc HeaderCarrier for HTTP context
   * @return Future[Either[UpstreamErrorResponse, Unit]]
   */
  def submitToNrs(
    payload: NrsPayload
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    nrsConnector.submitToNrs(payload).map {
      case Right(_)    =>
        logger.info(s"Successfully submitted to NRS for notable event: ${payload.metadata.notableEvent}")
        Right(())
      case Left(error) =>
        logger.error(s"Failed to submit to NRS for notable event: ${payload.metadata.notableEvent} - ${error.getMessage}")
        Left(error)
    }
  }
}
