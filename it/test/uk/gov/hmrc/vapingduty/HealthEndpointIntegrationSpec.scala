package uk.gov.hmrc.vapingduty

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{StringContextOps, HttpResponse, HttpReads, HeaderCarrier}
import uk.gov.hmrc.http.HttpReads.Implicits._

class HealthEndpointIntegrationSpec
  extends AnyWordSpec
     with Matchers
     with ScalaFutures
     with IntegrationPatience
     with GuiceOneServerPerSuite:

  private val httpClient = app.injector.instanceOf[HttpClientV2]
  private val baseUrl  = s"http://localhost:$port"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .build()

  "service health endpoint" should:
    "respond with 200 status" in:
      val response: Future[HttpResponse] =
        httpClient
          .get(url"$baseUrl/ping/ping")(HeaderCarrier())
          .execute()

      response.futureValue.status shouldBe 200
