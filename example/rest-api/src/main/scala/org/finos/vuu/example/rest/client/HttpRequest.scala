package org.finos.vuu.example.rest.client

import org.finos.vuu.example.rest.client.HttpRequestBuilder.{Header, QueryParam}

import scala.collection.mutable.ListBuffer

case class HttpRequest(baseUrl: String,
                       requestPath: Option[String],
                       headers: Map[String, String],
                       body: Option[String],
                       queryParams: Map[String, String])

object HttpRequestBuilder {
  def apply(baseUrl: String): HttpRequestBuilder = {
    new HttpRequestBuilder(
      baseUrl,
      requestPath = Option.empty,
      body = Option.empty,
      headers = ListBuffer.empty,
      queryParams = ListBuffer.empty
    )

  }

  type Header = (String, String)
  type QueryParam = (String, String)
}

class HttpRequestBuilder private (val baseUrl: String,
                                  private val requestPath: Option[String],
                                  private val body: Option[String],
                                  private val headers: ListBuffer[Header],
                                  private val queryParams: ListBuffer[QueryParam]) {

  def withBody(body: String): HttpRequestBuilder = this.copy(body = Some(body))
  def withRequestPath(path: String): HttpRequestBuilder = this.copy(requestPath = Some(path))
  def withQueryParam(param: QueryParam): HttpRequestBuilder = this.copy(queryParams = queryParams.addOne(param))
  def withHeader(header: Header): HttpRequestBuilder = this.copy(headers = headers.addOne(header))

  def build(): HttpRequest = {
    HttpRequest(
      baseUrl = baseUrl,
      requestPath = requestPath,
      headers = headers.toMap,
      body = body,
      queryParams = queryParams.toMap
    )
  }

  private def copy(requestPath: Option[String] = this.requestPath,
                   body: Option[String] = this.body,
                   headers: ListBuffer[Header] = this.headers,
                   queryParams: ListBuffer[QueryParam] = this.queryParams) = {
    new HttpRequestBuilder(this.baseUrl, requestPath, body, headers, queryParams)
  }
}
