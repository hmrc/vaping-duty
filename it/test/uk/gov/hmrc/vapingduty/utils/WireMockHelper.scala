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

package uk.gov.hmrc.vapingduty.utils

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.matching.{EqualToJsonPattern, EqualToPattern}
import org.scalatest.Suite
import uk.gov.hmrc.http.test.WireMockSupport

trait WireMockHelper extends WireMockSupport {
  this: Suite =>

  protected val endpointConfigurationPath = "microservice.services"

  protected def getWireMockAppConfig(endpointNames: Seq[String]): Map[String, Any] =
    endpointNames
      .flatMap(endpointName =>
        Seq(
          s"$endpointConfigurationPath.$endpointName.host" -> wireMockHost,
          s"$endpointConfigurationPath.$endpointName.port" -> wireMockPort
        )
      )
      .toMap

  private def stripToPath(url: String) =
    if (url.startsWith("http://") || url.startsWith("https://")) {
      url.dropWhile(_ != '/').dropWhile(_ == '/').dropWhile(_ != '/')
    } else {
      url
    }

  private def urlWithParameters(url: String, parameters: Seq[(String, String)]) = {
    val queryParams = parameters.map { case (k, v) => s"$k=$v" }.mkString("&")

    s"${stripToPath(url)}?$queryParams"
  }

  def stubGet(url: String, status: Int, body: String): Unit =
    wireMockServer.stubFor(
      WireMock.get(urlEqualTo(stripToPath(url))).willReturn(aResponse().withStatus(status).withBody(body))
    )

  def stubGetFault(url: String, fault: Fault = Fault.EMPTY_RESPONSE): Unit =
    wireMockServer.stubFor(
      WireMock.get(urlEqualTo(stripToPath(url))).willReturn(aResponse().withFault(fault))
    )

  def stubGetWithParameters(url: String, parameters: Seq[(String, String)], status: Int, body: String): Unit =
    wireMockServer.stubFor(
      WireMock
        .get(urlEqualTo(urlWithParameters(url, parameters)))
        .willReturn(aResponse().withStatus(status).withBody(body))
    )

  def stubGetFaultWithParameters(url: String, parameters: Seq[(String, String)], fault: Fault = Fault.EMPTY_RESPONSE): Unit =
    wireMockServer.stubFor(
      WireMock
        .get(urlEqualTo(urlWithParameters(url, parameters)))
        .willReturn(aResponse().withFault(fault))
    )

  def stubPost(url: String, status: Int, requestBody: String, returnBody: String): Unit =
    wireMockServer.stubFor(
      WireMock
        .post(urlEqualTo(stripToPath(url)))
        .withRequestBody(new EqualToJsonPattern(requestBody, true, false))
        .willReturn(aResponse().withStatus(status).withBody(returnBody))
    )

  def stubPostFault(url: String, requestBody: String, fault: Fault = Fault.EMPTY_RESPONSE): Unit =
    wireMockServer.stubFor(
      WireMock
        .post(urlEqualTo(stripToPath(url)))
        .withRequestBody(new EqualToJsonPattern(requestBody, true, false))
        .willReturn(aResponse().withFault(fault))
    )

  def stubPut(url: String, status: Int, requestBody: String, returnBody: String): Unit =
    wireMockServer.stubFor(
      WireMock
        .put(urlEqualTo(stripToPath(url)))
        .withRequestBody(new EqualToJsonPattern(requestBody, true, false))
        .willReturn(aResponse().withStatus(status).withBody(returnBody))
    )

  def verifyGet(url: String): Unit =
    wireMockServer.verify(getRequestedFor(urlEqualTo(stripToPath(url))))

  def verifyGetWithParameters(url: String, parameters: Seq[(String, String)]): Unit =
    wireMockServer.verify(getRequestedFor(urlEqualTo(urlWithParameters(url, parameters))))

  def verifyGetWithParametersAndHeaders(url: String,
                                        parameters: Seq[(String, String)] = Seq.empty,
                                        headers: Seq[(String, String)] = Seq.empty): Unit = {

    val requestPattern            = getRequestedFor(urlEqualTo(urlWithParameters(url, parameters)))
    val requestPatternWithHeaders = headers.foldLeft(requestPattern) { (pattern, header) =>
      pattern.withHeader(header._1, new EqualToPattern(header._2))
    }
    wireMockServer.verify(requestPatternWithHeaders)
  }
  
  def verifyPost(url: String): Unit =
    wireMockServer.verify(postRequestedFor(urlEqualTo(stripToPath(url))))

  def verifyPut(url: String): Unit =
    wireMockServer.verify(putRequestedFor(urlEqualTo(stripToPath(url))))
  
}
