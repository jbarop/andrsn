package io.andrsn.http

import com.dslplatform.json.DslJson
import io.andrsn.matrix.supportedVersions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.core.net.SocketAddress.inetSocketAddress
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.RoutingContext
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess

fun startHttpServer() {
  with(Vertx.vertx()) {
    deployVerticle(HttpServer())
  }
}

private class HttpServer : AbstractVerticle() {

  private val json = DslJson<Any>()

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
      route().handler(corsHandler())
      route().handler(logRequestHandler())
      get("/_matrix/client/versions").handler(versionsHandler())
      route().handler(notFoundHandler())
    }

  private fun versionsHandler(): Handler<RoutingContext> =
    Handler { ctx ->
      val baos = ByteArrayOutputStream()
      json.serialize(supportedVersions, baos)
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(Buffer.buffer(baos.toByteArray()))
    }

  private fun logRequestHandler(): Handler<RoutingContext> =
    Handler { ctx ->
      println("Request: ${ctx.request().method()} ${ctx.request().uri()}")
      ctx.next()
    }

  private fun corsHandler(): Handler<RoutingContext> =
    Handler { ctx ->
      ctx.response().putHeader("Access-Control-Allow-Origin", "*")
      ctx.response().putHeader(
        "Access-Control-Allow-Methods",
        "GET,POST,PUT,DELETE,OPTIONS"
      )
      ctx.response().putHeader(
        "Access-Control-Allow-Headers",
        "Authorization,Content-Type,X-Requested-With"
      )
      ctx.response().putHeader("Access-Control-Allow-Credentials", "true")

      if (ctx.request().method() == HttpMethod.OPTIONS) {
        ctx.response().setStatusCode(204).end()
      } else {
        ctx.next()
      }
    }

  private fun notFoundHandler(): Handler<RoutingContext> =
    Handler { ctx ->
      val error = JsonObject()
        .put("errcode", "M_NOT_FOUND")
        .put("error", "No resource was found for this request.")
      ctx.response()
        .setStatusCode(404)
        .putHeader("Content-Type", "application/json")
        .end(error.encode())
    }

}
