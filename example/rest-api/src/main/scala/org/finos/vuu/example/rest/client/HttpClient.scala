package org.finos.vuu.example.rest.client

import org.finos.vuu.example.rest.client.HttpClient.Handler

import scala.util.Try

trait HttpClient {
  def get(request: HttpRequest): Handler[ClientResponse] => Unit
}

object HttpClient {
  type Handler[T] = Try[T] => Unit
  def apply(mock: Boolean = false): HttpClient = {
    if (mock) FakeHttpClient() else VertXClient()
  }
}

case class ClientResponse(body: String, statusCode: Int)
