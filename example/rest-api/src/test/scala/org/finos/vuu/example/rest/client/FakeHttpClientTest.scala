package org.finos.vuu.example.rest.client

import org.finos.toolbox.json.JsonUtil
import org.finos.vuu.example.rest.TestUtils.jsonArrayRegex
import org.finos.vuu.example.rest.model.Instrument
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.util.Try

class FakeHttpClientTest extends AnyFeatureSpec with Matchers {
  private val fakeHttpClient = FakeHttpClient()

  private val requestBuilder = HttpRequestBuilder(s"mock-url.com")

  Feature("get") {
     Scenario("supports /instruments endpoint") {

       fakeHttpClient.register(Endpoints.getInstruments)
       var response: Try[ClientResponse] = null

       val req = requestBuilder.withRequestPath("/instruments").withQueryParam("limit" -> "2").build()
       fakeHttpClient.get(req) { response = _ }

       response.get.body should include regex jsonArrayRegex(2)
       JsonUtil.fromJson[List[Instrument]](response.get.body).head shouldBe a [Instrument]
     }

    Scenario("returns 404 when endpoint not found") {

      var response: Try[ClientResponse] = null

      val req = requestBuilder.withRequestPath("/endpoint-not-found").build()
      fakeHttpClient.get(req) { response = _ }

      response.isSuccess shouldEqual true
      response.get.statusCode shouldEqual 404
    }

    Scenario("`/identity` endpoint returns passed body and status code query param") {

      fakeHttpClient.register(Endpoints.identity)
      var response: Try[ClientResponse] = null

      val req = requestBuilder
        .withRequestPath("/identity")
        .withBody("some dummy body")
        .withQueryParam("statusCode", "403")
        .build()
      fakeHttpClient.get(req) { response = _ }

      response.isSuccess shouldEqual true
      response.get.statusCode shouldEqual 403
      response.get.body shouldEqual  "some dummy body"
    }

    Scenario("`/identity` endpoint returns passed body query param and status code query param " +
      "WHEN no body present in the request") {
      fakeHttpClient.register(Endpoints.identity)
      var response: Try[ClientResponse] = null

      val req = requestBuilder
        .withRequestPath("/identity")
        .withQueryParam("body", "some dummy body from query param")
        .withQueryParam("statusCode", "500")
        .build()
      fakeHttpClient.get(req) {
        response = _
      }

      response.isSuccess shouldEqual true
      response.get.statusCode shouldEqual 500
      response.get.body shouldEqual "some dummy body from query param"
    }

     Scenario("`/failure` endpoint acts as if exception occurred when making request") {
       fakeHttpClient.register(Endpoints.failure)
       var response: Try[ClientResponse] = null

       val req = requestBuilder.withRequestPath("/failure").build()
       fakeHttpClient.get(req) { response = _ }

       response.isFailure shouldEqual true
       response.failed.get shouldBe a [Exception]
     }
   }

  Feature("Endpoints regex") {
    forAll(Table(
      ("url", "expected"),
      ("/instruments", true),
      ("/instruments/", true),
      ("/instruments//", true),
      ("/instrument", false),
      ("/instrumentsX", false),
      ("/instruments/abc123", false),
    ))((url, expected) => {
      val subdirName = "instruments"
      Scenario(s"endpointRegexStrict should return $expected when passed url is $url and subdir is $subdirName") {
        Endpoints.endpointRegexStrict(subdirName).matches(url) shouldEqual expected
      }
    })

    forAll(Table(
      ("url", "expected"),
      ("/failure", true),
      ("/failure//", true),
      ("/failure/abc123", true),
      ("/failures", false),
    ))((url, expected) => {
      val subdirName = "failure"
      Scenario(s"endpointRegexLoose should return $expected when passed url is $url and subdir is $subdirName") {
        Endpoints.endpointRegexLoose(subdirName).matches(url) shouldEqual expected
      }
    })
  }
}
