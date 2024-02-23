package org.finos.vuu.example.rest.client

import io.vertx.core.{MultiMap, Vertx}
import io.vertx.core.http.impl.headers.HeadersMultiMap
import io.vertx.ext.web.client.WebClient
import io.vertx.uritemplate.UriTemplate
import io.vertx.ext.web.client.{HttpRequest => VertxHttpRequest}

import scala.jdk.CollectionConverters._
import java.nio.charset.Charset
import scala.util.{Failure, Success, Try}
class NewVertXClient(baseUrl: String) {
  private val webClient = WebClient.create(Vertx.vertx())

  def get(request: HttpRequest): Try[ClientResponse] => Unit = _ => {
    val vertxRequest = createVertxRequest(request)

    vertxRequest
      .send()
      .onSuccess(res => {
        val bodyStr = Try(res.body.toString(Charset.forName("UTF-8"))).getOrElse("")
        Success(ClientResponse(bodyStr, res.statusCode()))
      })
      .onFailure(cause => Failure(cause))
  }


  private def createVertxRequest(request: HttpRequest) = {
    val vertxRequest = webClient.getAbs(UriTemplate.of(baseUrl + request.requestPath.getOrElse("")))
    addQueryParams(vertxRequest, request)
    vertxRequest.putHeaders(httpHeaders(request))
    vertxRequest
  }

  private def addQueryParams[T](vertxRequest: VertxHttpRequest[T], request: HttpRequest): VertxHttpRequest[T] = {
    request.queryParams.foreach({ case (name, value) => vertxRequest.addQueryParam(name, value) })
    vertxRequest
  }

  private def httpHeaders(request: HttpRequest): MultiMap = {
    HeadersMultiMap.httpHeaders().addAll(request.headers.asJava)
  }

}
