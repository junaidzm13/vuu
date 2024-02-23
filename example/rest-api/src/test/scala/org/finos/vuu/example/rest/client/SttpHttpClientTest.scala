package org.finos.vuu.example.rest.client

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import sttp.client4.UriContext
import sttp.model.Uri

class SttpHttpClientTest extends AnyFeatureSpec with Matchers {

  //https://sttp.softwaremill.com/en/latest/testing.html
  //https://sttp.softwaremill.com/en/latest/backends/akka.html
  Scenario("simple get endpoint") {

    val url: Uri = uri"http://google.com"

    val httpClient = new SttpHttpClient()
    val response = httpClient.get(url)

    response.statusText shouldEqual 200
    response.body shouldEqual "some dummy body"
  }

}
