package io.andrsn.http

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.core.net.SocketAddress.inetSocketAddress
import io.vertx.ext.web.Router.router
import kotlin.system.exitProcess

fun startHttpServer() {
  with(Vertx.vertx()) {
    deployVerticle(HttpServer())
  }
}

private class HttpServer : AbstractVerticle() {

  override fun start() {
    val bindAddress = inetSocketAddress(8443, "localhost")
    val options = HttpServerOptions()
      .setSsl(true)
      .setHost(bindAddress.host())
      .setPort(bindAddress.port())
      .setKeyCertOptions(
        PemKeyCertOptions()
          .setKeyPath("localhost-key.pem")
          .setCertPath("localhost.pem")
      )

    vertx.createHttpServer(options)
      .requestHandler(createRouter())
      .listen()
      .onSuccess {
        println("HTTP server listening on $bindAddress")
      }
      .onFailure {
        println("Failed to start HTTP server: ${it.localizedMessage}")
        it.printStackTrace()
        exitProcess(1)
      }
  }

  private fun createRouter() =
    router(vertx).apply {
      get("/").handler { ctx -> ctx.response().end("Hello world!") }
      route().handler { ctx ->
        val error = JsonObject()
          .put("errcode", "M_NOT_FOUND")
          .put("error", "No resource was found for this request.")
        ctx.response()
          .setStatusCode(404)
          .putHeader("Content-Type", "application/json")
          .end(error.encode())
      }
    }

}
