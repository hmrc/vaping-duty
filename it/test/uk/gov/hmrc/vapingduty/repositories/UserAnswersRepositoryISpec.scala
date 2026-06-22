/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC
import play.api.libs.json.Json
import uk.gov.hmrc.mdc.MdcExecutionContext
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.vapingduty.config.AppConfig
import uk.gov.hmrc.vapingduty.models.*
import uk.gov.hmrc.vapingduty.models.identifiers.{PeriodKey, VpdId}
import utils.TestData

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, Month}
import scala.concurrent.{ExecutionContext, Future}

class UserAnswersRepositoryISpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterAll
    with TestData {

  private val stubClock: Clock = clock
  private val instant = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS)

  private val testVpdId = VpdId("GBWK0000000WK")
  private val testPeriodKey = PeriodKey("26AB")
  private val userAnswers = UserAnswers(testVpdId.id, testPeriodKey.value, Some(Month.JUNE), Some("2027"), Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.timeToLive) thenReturn 1L

  implicit val productionLikeTestMdcExecutionContext: ExecutionContext = MdcExecutionContext()

  protected override val repository: UserAnswersRepository = new UserAnswersRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  override def beforeEach(): Unit = repository.clear(VpdId(userAnswers.vpdId), PeriodKey(userAnswers.periodKey))
  
  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = userAnswers copy (lastUpdated = instant)

      repository.set(userAnswers.copy(lastUpdated = instant)).futureValue

      val updatedRecord = find(Filters.and(
        Filters.equal("vpdId", userAnswers.vpdId),
        Filters.equal("periodKey", userAnswers.periodKey)
      )).futureValue.headOption.value

      updatedRecord mustEqual expectedResult
    }

    "must return UpdateSuccess when updating an existing document" in {

      val result = repository.set(userAnswers.copy(data = Json.obj(), lastUpdated = instant)).futureValue

      result mustBe UpdateSuccess

      val updatedResult = repository.set(userAnswers.copy(data = Json.obj("foo" -> "bar"))).futureValue

      updatedResult mustBe UpdateSuccess
    }

    "must return UpdateSuccess when updating an existing document that is identical" in {

      val result = repository.set(userAnswers.copy(data = Json.obj())).futureValue

      result mustBe UpdateSuccess

      val updatedResult = repository.set(userAnswers.copy(data = Json.obj())).futureValue

      updatedResult mustBe UpdateSuccess
    }

    mustPreserveMdc(repository.set(userAnswers))
  }

  private val badKey = PeriodKey("99ZZ")

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        insert(userAnswers).futureValue

        val result = repository.get(testVpdId, testPeriodKey).futureValue
        val expectedResult = userAnswers copy (lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get(testVpdId, badKey).futureValue must not be defined
      }
    }

    mustPreserveMdc(repository.get(testVpdId, badKey))
  }

  ".clear" - {

    "must remove a record" in {

      insert(userAnswers).futureValue

      val result = repository.clear(VpdId(userAnswers.vpdId), PeriodKey(userAnswers.periodKey)).futureValue

      repository.get(testVpdId, testPeriodKey).futureValue must not be defined

      result mustBe UpdateSuccess
    }

    "must return UpdateFailure when there is no record to remove" in {
      val result = repository.clear(testVpdId, badKey).futureValue

      result mustEqual UpdateFailure
    }

    mustPreserveMdc(repository.clear(VpdId(userAnswers.vpdId), PeriodKey(userAnswers.periodKey)))
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {

        insert(userAnswers).futureValue

        val result = repository.keepAlive(testVpdId, testPeriodKey).futureValue

        val expectedUpdatedAnswers = userAnswers copy (lastUpdated = instant)

        val updatedAnswers = find(Filters.and(
          Filters.equal("vpdId", userAnswers.vpdId),
          Filters.equal("periodKey", userAnswers.periodKey)
        )).futureValue.headOption.value

        updatedAnswers mustEqual expectedUpdatedAnswers
        result mustBe UpdateSuccess
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        val result = repository.keepAlive(testVpdId, badKey).futureValue

        result mustEqual UpdateFailure
      }
    }

    mustPreserveMdc(repository.keepAlive(testVpdId, testPeriodKey))
  }

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      MDC.put("test", "foo")

      f.map { _ =>
        Option(MDC.get("test"))
      }.futureValue mustEqual Some("foo")
    }
}