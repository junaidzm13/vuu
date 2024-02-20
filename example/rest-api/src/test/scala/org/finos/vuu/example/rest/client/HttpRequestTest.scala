package org.finos.vuu.example.rest.client

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers

class HttpRequestTest extends AnyFeatureSpec with Matchers {
  private val baseUrl = "base-url.com"

  Feature("HttpRequestBuilder") {
    Scenario("Can build request with default empty values") {
      val request = HttpRequestBuilder(baseUrl).build()

      request shouldEqual httpRequest(baseUrl = baseUrl)
    }

    Scenario("Can build request with provided body") {
      val request = HttpRequestBuilder(baseUrl).withBody("some-body").build()

      request shouldEqual httpRequest(baseUrl = baseUrl, body = Some("some-body"))
    }

    Scenario("Can build request with provided request path") {
      val request = HttpRequestBuilder(baseUrl).withRequestPath("/entities").build()

      request shouldEqual httpRequest(baseUrl = baseUrl, requestPath = Some("/entities"))
    }

    Scenario("Can build request with provided headers") {
      val headers = Map("header1" -> "value1", "header2" -> "value2")

      val request = HttpRequestBuilder(baseUrl)
        .withHeader(headers.head)
        .withHeader(headers.last)
        .build()

      request shouldEqual httpRequest(baseUrl = baseUrl, headers = headers)
    }

    Scenario("Can build request with provided query params") {
      val queryParams = Map("param1" -> "value1", "param2" -> "value2")

      val request = HttpRequestBuilder(baseUrl)
        .withQueryParam(queryParams.head)
        .withQueryParam(queryParams.last)
        .build()

      request shouldEqual httpRequest(baseUrl = baseUrl, queryParams = queryParams)
    }
  }

  private def httpRequest(baseUrl: String = "test-url.com",
                           requestPath: Option[String] = None,
                           headers: Map[String, String] = Map.empty,
                           body: Option[String] = None,
                           queryParams: Map[String, String] = Map.empty) = {
    HttpRequest(baseUrl = baseUrl, requestPath = requestPath, headers = headers, body = body, queryParams = queryParams)
  }
}

