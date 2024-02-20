package org.finos.vuu.example.rest

import org.finos.toolbox.json.JsonUtil
import org.finos.vuu.example.rest.client.{ClientResponse, HttpClient, HttpRequestBuilder}
import org.finos.vuu.example.rest.client.HttpClient.Handler
import org.finos.vuu.example.rest.model.Instrument

import scala.util.{Failure, Try}

trait InstrumentServiceClient {
  def getInstruments(limit: Int)(handler: Try[List[Instrument]] => Unit): Unit
}

private class InstrumentServiceClientImpl(httpClient: HttpClient, baseUrl: String) extends InstrumentServiceClient {
  private val baseRequestBuilder = HttpRequestBuilder(baseUrl)

  def getInstruments(limit: Int)(handler: Handler[List[Instrument]]): Unit = {
    val request = baseRequestBuilder
      .withRequestPath("/instruments")
      .withQueryParam("limit", limit.toString)
      .build()

    httpClient.get(request) { res =>
      val instruments = res.flatMap({
        case ClientResponse(body, 200) => Try(JsonUtil.fromJson[List[Instrument]](body))
        case ClientResponse(_, code) => Failure(new Exception(s"Error occurred with status code: $code, expected 200"))
      })
      handler(instruments)
    }
  }
}

object InstrumentServiceClient {
  def apply(httpClient: HttpClient, baseUrl: String): InstrumentServiceClient = {
    new InstrumentServiceClientImpl(httpClient, baseUrl)
  }
}
