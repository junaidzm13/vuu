package org.finos.vuu.example.rest.demoserver

import io.vertx.core.{AbstractVerticle, Vertx}
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

object DemoRestServer extends App {
  private val options = DemoRestServerOptions(8080, "localhost")
  Vertx.vertx().deployVerticle(new DemoRestServer(options))
}

class DemoRestServer(options: DemoRestServerOptions) extends AbstractVerticle {
  override def start(): Unit = {
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.route("/instrument-service/*").subRouter(InstrumentRouter(vertx))
    server.requestHandler(router).listen(options.port, options.host)
  }
}

case class DemoRestServerOptions(port: Int, host: String)
