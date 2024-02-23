package org.finos.vuu.example.rest.client

import io.vertx.core.{MultiMap, Vertx}
import io.vertx.core.http.impl.headers.HeadersMultiMap
import io.vertx.ext.web.client.WebClient
import io.vertx.uritemplate.UriTemplate
import io.vertx.ext.web.client.{HttpRequest => VertxHttpRequest}
import org.finos.vuu.example.rest.client.HttpClient.Handler

import scala.jdk.CollectionConverters._
import java.nio.charset.Charset
import scala.util.{Failure, Success, Try}

private object VertXClient {
  private val rawClient = WebClient.create(Vertx.vertx())

  def apply(): VertXClient = {
    new VertXClient(rawClient)
  }
}

private class VertXClient(rawClient: WebClient) extends HttpClient {
  override def get(request: HttpRequest): Handler[ClientResponse] => Unit = {
    handler => {
      val vertxRequest = rawClient.getAbs(UriTemplate.of(request.baseUrl + request.requestPath.getOrElse("")))

      addQueryParams(vertxRequest, request)
        .putHeaders(httpHeaders(request))
        .send()
        .onSuccess(res => {
          val bodyStr = Try(res.body.toString(Charset.forName("UTF-8"))).getOrElse("")
          handler(Success(ClientResponse(bodyStr, res.statusCode())))
        })
        .onFailure(cause => handler(Failure(cause)))
    }
  }

  private def addQueryParams[T](vertxRequest: VertxHttpRequest[T], request: HttpRequest): VertxHttpRequest[T] = {
    request.queryParams.foreach({ case (name, value) => vertxRequest.addQueryParam(name, value) })
    vertxRequest
  }

  private def httpHeaders(request: HttpRequest): MultiMap = {
    HeadersMultiMap.httpHeaders().addAll(request.headers.asJava)
  }
}