package org.finos.vuu.example.rest.client

import org.finos.toolbox.json.JsonUtil.toRawJson
import org.finos.vuu.example.rest.client.FakeHttpClient.Endpoint
import org.finos.vuu.example.rest.client.HttpClient.Handler
import org.finos.vuu.example.rest.demoserver.InstrumentRouter.DEFAULT_LIMIT
import org.finos.vuu.example.rest.model.RandomInstrument

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

object FakeHttpClient {
  def apply(): FakeHttpClient = {
    new FakeHttpClient()
  }

  case class Endpoint(regex: Regex, handler: HttpRequest => Try[ClientResponse])
}

class FakeHttpClient extends HttpClient {

  private val endpoints =  ListBuffer.empty[Endpoint]

  def register(endpoint: Endpoint): Unit = {
    endpoints += endpoint
  }

  override def get(request: HttpRequest): Handler[ClientResponse] => Unit = {
    val requestPath = request.requestPath.getOrElse("")
    val response = endpoints
      .filter(_.regex.matches(requestPath))
      .collectFirst(_.handler(request))
      .getOrElse(Success(ClientResponse("Resource not found", 404)))
    handler => handler(response)
  }
}

object Endpoints {
   val getInstruments: Endpoint = Endpoint(
    endpointRegexStrict(subdirName = "instruments"),
    r => {
      val count = r.queryParams.get("limit").flatMap(_.toIntOption).getOrElse(DEFAULT_LIMIT)
      Success(ClientResponse(toRawJson(RandomInstrument.create(size = count)), 200))
    }
  )
  val failure: Endpoint = Endpoint(
    endpointRegexLoose(subdirName = "failure"),
    _ => Failure(new Exception("Error occurred when making request"))
  )
  val identity: Endpoint = Endpoint(
    endpointRegexLoose(subdirName = "identity"),
    r => {
      val body = r.body.getOrElse(r.queryParams.getOrElse("body", ""))
      val statusCode = r.queryParams.get("statusCode").flatMap(_.toIntOption).getOrElse(400)
      Success(ClientResponse(body, statusCode))
    }
  )

  def endpointRegexLoose(subdirName: String): Regex = s"^/$subdirName(/+.*)?".r
  def endpointRegexStrict(subdirName: String): Regex = s"^/$subdirName/{0,2}$$".r
}
