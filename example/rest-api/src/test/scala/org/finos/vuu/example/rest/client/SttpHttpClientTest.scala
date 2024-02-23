package org.finos.vuu.example.rest.client

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import sttp.client4.UriContext
import sttp.model.{StatusCode, Uri}

class SttpHttpClientTest extends AnyFeatureSpec with Matchers {

  //https://docs.scala-lang.org/toolkit/http-client-request.html#the-request-definition
  //https://sttp.softwaremill.com/en/latest/testing.html
  //https://sttp.softwaremill.com/en/latest/backends/akka.html
  Feature("Http client using sttp"){

    ignore("simple get endpoint") {

      val url: Uri = uri"http://google.com" //todo replace with fake url we want to test with

      val httpClient = new SttpHttpClient()
      val response = httpClient.get(url)

      response.code shouldEqual StatusCode.Ok
      response.body shouldEqual "some dummy body"
    }
  }


}
