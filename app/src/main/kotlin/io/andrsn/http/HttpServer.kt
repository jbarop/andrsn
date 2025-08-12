package io.andrsn.http

import io.andrsn.matrix.Matrix
import io.andrsn.matrix.dto.MatrixRequest
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer.buffer
import io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
import io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS
import io.vertx.core.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
import io.vertx.core.http.HttpHeaders.AUTHORIZATION
import io.vertx.core.http.HttpHeaders.CONTENT_TYPE
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.core.net.SocketAddress.inetSocketAddress
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.RoutingContext
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess

/**
 * Starts the HTTP server.
 *
 * It exposes Matrix API endpoints and handles CORS. Business logic is delegated
 * to a [Matrix] instance.
 *
 * @param matrix The [Matrix] instance responsible for processing API requests.
 */
fun startHttpServer(matrix: Matrix) {
  with(Vertx.vertx()) {
    deployVerticle(HttpServer(matrix))
  }
}

/**
 * HTTP server implementation using Vert.x for handling HTTP requests.
 */
private class HttpServer(
  private val matrix: Matrix,
) : AbstractVerticle() {

  override fun start() {
    val bindAddress = inetSocketAddress(8443, "localhost")
    val options = HttpServerOptions()
      .setSsl(true)
      .setHost(bindAddress.host())
      .setPort(bindAddress.port())
      .setKeyCertOptions(
        PemKeyCertOptions()
          .setKeyPath("localhost-key.pem")
          .setCertPath("localhost.pem"),
      )

    vertx
      .createHttpServer(options)
      .requestHandler(createRouter())
      .listen()
      .onSuccess {
        println("HTTP server listening on $bindAddress")
      }.onFailure {
        println("Failed to start HTTP server: ${it.localizedMessage}")
        it.printStackTrace()
        exitProcess(1)
      }
  }

  private fun createRouter() =
    router(vertx).apply {
      route().handler(corsHandler())
      route().handler(logRequestHandler())
      route().matrixHandler()
    }

  private fun Route.matrixHandler() {
    this.handler { ctx ->
      ctx.request().bodyHandler { body ->
        val request = MatrixRequest()
        request.method = ctx.request().path()
        request.requestBody = body.bytes
        request.responseStream = ByteArrayOutputStream()
        request.finishResponse = { statusCode: Int ->
          ctx
            .response()
            .setStatusCode(statusCode)
            .putHeader(
              CONTENT_TYPE,
              APPLICATION_JSON,
            ).end(buffer(request.responseStream.toByteArray()))
        }

        matrix.handleEvent(request)
      }
    }
  }

  private fun logRequestHandler(): Handler<RoutingContext> =
    Handler { ctx ->
      println("Request: ${ctx.request().method()} ${ctx.request().uri()}")
      ctx.next()
    }

  private fun corsHandler(): Handler<RoutingContext> =
    Handler { ctx ->
      ctx.response().putHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
      ctx.response().putHeader(
        ACCESS_CONTROL_ALLOW_METHODS,
        "GET,POST,PUT,DELETE,OPTIONS",
      )
      ctx.response().putHeader(
        ACCESS_CONTROL_ALLOW_HEADERS,
        "$AUTHORIZATION,$CONTENT_TYPE,X-Requested-With",
      )
      ctx.response().putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")

      if (ctx.request().method() == HttpMethod.OPTIONS) {
        ctx.response().setStatusCode(204).end()
      } else {
        ctx.next()
      }
    }
}
