package org.finos.vuu.example.rest.client

import io.vertx.uritemplate.UriTemplate
import org.finos.toolbox.json.JsonUtil
import org.finos.vuu.example.rest.TestUtils.jsonArrayRegex
import org.finos.vuu.example.rest.client.FakeHttpClient.UnsupportedEndpointException
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
       var response: Try[ClientResponse] = null

       val req = requestBuilder.withRequestPath("/instruments").withQueryParam("limit" -> "2").build()
       fakeHttpClient.get(req) { response = _ }

       response.get.body should include regex jsonArrayRegex(2)
       JsonUtil.fromJson[List[Instrument]](response.get.body).head shouldBe a [Instrument]
     }

     Scenario("returns failure when unsupported endpoint") {
       var response: Try[ClientResponse] = null

       val req = requestBuilder.withRequestPath("/unsupported-endpoint").build()
       fakeHttpClient.get(req) { response = _ }

       response.isFailure shouldEqual true
       response.failed.get shouldBe a [UnsupportedEndpointException]
     }
   }

  Feature("EndpointRegex") {
    forAll(Table(
      ("url", "expected"),
      ("/instruments", true),
      ("/instruments/", true),
      ("/hello-world", false),
      ("/instrumentsX", false),
    ))((url, expected) => {
      Scenario(s"instruments endpoint regex should return $expected when passed url is $url") {
        EndpointRegex.instruments.matches(url) shouldEqual expected
      }
    })
  }
}
