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

package uk.gov.hmrc.vapingduty.repositories

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import play.api.libs.json.Format
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.models.identifiers.*
import uk.gov.hmrc.vapingduty.models.{UpdateFailure, UpdateResult, UpdateSuccess, UserAnswers}

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserAnswersRepository @Inject()(
                                       mongoComponent: MongoComponent,
                                       appConfig: AppConfig,
                                       clock: Clock
                                  )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UserAnswers](
    collectionName = "user-answers",
    mongoComponent = mongoComponent,
    domainFormat = UserAnswers.format,
    indexes = Seq(
      IndexModel(
        Indexes.compoundIndex(
          Indexes.ascending("vpdId"),
          Indexes.ascending("periodKey")
        ),
        IndexOptions()
          .name("vpdIdPeriodKeyIdx")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.timeToLive, TimeUnit.DAYS)
      )
    ),
    replaceIndexes = false
  ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byVpdIdPeriod(vpdId: VpdId, periodKey: PeriodKey): Bson = Filters.and(
    Filters.equal("vpdId", vpdId.toString),
    Filters.equal("periodKey", periodKey.toString)
  )
  

  def get(vpdId: VpdId, periodKey: PeriodKey): Future[Option[UserAnswers]] =
    keepAlive(vpdId, periodKey).flatMap { _ =>
      Mdc.preservingMdc {
        collection
          .find(byVpdIdPeriod(vpdId, periodKey))
          .headOption()
      }
    }

  def set(answers: UserAnswers): Future[UpdateResult] = {

    val updatedAnswers = answers copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byVpdIdPeriod(VpdId(answers.vpdId), PeriodKey(answers.periodKey)),
        replacement = updatedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(res =>
        if (Option(res.getUpsertedId).isDefined || res.getModifiedCount > 0) UpdateSuccess else UpdateFailure
      )
  }

  def keepAlive(vpdId: VpdId, periodKey: PeriodKey): Future[UpdateResult] =
    collection
      .updateOne(
        filter = byVpdIdPeriod(vpdId, periodKey),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(res => if (res.getModifiedCount > 0) UpdateSuccess else UpdateFailure)

  def clear(vpdId: VpdId, periodKey: PeriodKey): Future[UpdateResult] =
    collection
      .deleteOne(byVpdIdPeriod(vpdId, periodKey))
      .toFuture()
      .map(res => if (res.getDeletedCount > 0) UpdateSuccess else UpdateFailure)
}
