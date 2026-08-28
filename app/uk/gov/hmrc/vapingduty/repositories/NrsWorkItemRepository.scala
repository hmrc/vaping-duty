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

package uk.gov.hmrc.vapingduty.repositories

import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.models.nrs.NrsSubmissionWorkItem

import java.time.{Clock, Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsWorkItemRepository @Inject()(
                                       mongoComponent: MongoComponent,
                                       appConfig: AppConfig,
                                       clock: Clock
                                     )(using ec: ExecutionContext)
  extends WorkItemRepository[NrsSubmissionWorkItem](
    collectionName = "nrs-work-items",
    mongoComponent = mongoComponent,
    itemFormat = NrsSubmissionWorkItem.format,
    workItemFields = WorkItemFields.default
  ) {

  override def now(): Instant = Instant.now(clock)

  override lazy val inProgressRetryAfter: Duration =
    Duration.ofMillis(appConfig.nrsWorkItemRetryAfter.toMillis)

  override def completeAndDelete(id: org.bson.types.ObjectId): Future[Boolean] =
    super.completeAndDelete(id)

  override lazy val requiresTtlIndex: Boolean = true

  override def pullOutstanding(failedBefore: Instant, availableAt: Instant): Future[Option[uk.gov.hmrc.mongo.workitem.WorkItem[NrsSubmissionWorkItem]]] =
    super.pullOutstanding(failedBefore, availableAt)
}