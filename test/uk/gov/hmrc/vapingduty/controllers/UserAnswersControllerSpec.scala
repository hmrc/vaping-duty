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

package uk.gov.hmrc.vapingduty.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.api.test.FakeRequest
import uk.gov.hmrc.vapingduty.SpecBase
import uk.gov.hmrc.vapingduty.models.UserAnswers
import uk.gov.hmrc.vapingduty.repositories.UserAnswersRepository
import uk.gov.hmrc.vapingduty.models.identifiers.InternalId
import java.time.Instant

import scala.concurrent.Future

class UserAnswersControllerSpec extends SpecBase {
  private val internalId = InternalId("Int-52455-52524353f-34r34r43r")

  val mockUserAnswersRepository: UserAnswersRepository = mock[UserAnswersRepository]

  val controller = new UserAnswersController(
    cc,
    mockUserAnswersRepository,
    fakeAuthorisedAction,
    clock
  )

  val returnsUserAnswers = UserAnswers(
    id = internalId.toString,
    data = JsObject.empty,
    startedTime = Instant.now(),
    lastUpdated = Instant.now()
  )

  "getUserAnswers must" - {
    "return 200 OK with an existing user answers when there is one for the id" in {
      when(mockUserAnswersRepository.get(eqTo(internalId)))
        .thenReturn(Future.successful(Some(returnsUserAnswers)))

      val result: Future[Result] =
        controller.getUserAnswers(internalId)(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(returnsUserAnswers)
    }

    "return 404 NOT_FOUND when there is no user answers for the id" in {
      when(mockUserAnswersRepository.get(eqTo(internalId)))
        .thenReturn(Future.successful(None))

      val result: Future[Result] =
        controller.getUserAnswers(internalId)(fakeRequest)

      status(result) mustBe NOT_FOUND
    }
  }

  "set must" - {
    "return NO_CONTENT with the user answers that was updated" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val result: Future[Result] =
        controller.set()(
          fakeRequestWithJsonBody(Json.toJson(returnsUserAnswers.copy(lastUpdated = Instant.now().plusSeconds(1))))
        )

      status(result)        mustBe NO_CONTENT
      contentAsJson(result) mustBe Json.toJson(returnsUserAnswers)
    }

    "return 404 Not Found if the repository returns an error" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(false))

      val result: Future[Result] =
        controller.set()(
          fakeRequestWithJsonBody(Json.toJson("<not-json>"))
        )

      status(result) mustBe BAD_REQUEST
    }
  }

  "clear must" - {
    "return 204 NO_CONTENT" in {
      when(mockUserAnswersRepository.clear(any())).thenReturn(Future.successful(true))

      val result: Future[Result] = controller.clear(internalId)(FakeRequest())

      status(result) mustBe NO_CONTENT
    }
  }
}
