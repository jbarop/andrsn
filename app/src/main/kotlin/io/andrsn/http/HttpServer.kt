package io.andrsn.http

import com.dslplatform.json.DslJson
import io.andrsn.matrix.Matrix
import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.MatrixResponse
import io.andrsn.matrix.dto.RegisterRequest
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.VersionsResponse
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.core.net.SocketAddress.inetSocketAddress
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.RoutingContext
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess

fun startHttpServer(matrix: Matrix) {
  with(Vertx.vertx()) {
    deployVerticle(HttpServer(matrix))
  }
}

private class HttpServer(
  private val matrix: Matrix,
) : AbstractVerticle() {

  private val json = DslJson<Any>()

  override fun start() {
    val bindAddress = inetSocketAddress(8443, "localhost")
    val options =
      HttpServerOptions()
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
      get("/_matrix/client/versions")
        .handler<Unit, VersionsResponse> { matrix.getSupportedVersions() }
      get("/_matrix/client/v3/login")
        .handler<Unit, LoginTypesResponse> { matrix.getLoginTypes() }
      post("/_matrix/client/v3/register")
        .handler<RegisterRequest, RegisterResponse> { matrix.register(it) }
      route().handler(notFoundHandler())
    }

  private inline fun <reified RequestDto, ResponseDto> Route.handler(
    crossinline block: (dto: RequestDto) -> MatrixResponse<ResponseDto>,
  ) {
    this.handler { ctx ->
      ctx.request().bodyHandler { body ->
        val requestDto =
          if (RequestDto::class.java == Unit.javaClass) {
            Unit as RequestDto
          } else {
            json.deserialize(
              RequestDto::class.java,
              body.bytes,
              body.bytes.size,
            )
              ?: throw IllegalArgumentException("Failed to deserialize request")
          }

        val matrixResponse = block(requestDto)
        val baos = ByteArrayOutputStream()
        json.serialize(matrixResponse.data, baos)
        ctx
          .response()
          .setStatusCode(matrixResponse.statusCode)
          .putHeader("Content-Type", "application/json")
          .end(Buffer.buffer(baos.toByteArray()))
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
      ctx.response().putHeader("Access-Control-Allow-Origin", "*")
      ctx.response().putHeader(
        "Access-Control-Allow-Methods",
        "GET,POST,PUT,DELETE,OPTIONS",
      )
      ctx.response().putHeader(
        "Access-Control-Allow-Headers",
        "Authorization,Content-Type,X-Requested-With",
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
      val error =
        JsonObject()
          .put("errcode", "M_NOT_FOUND")
          .put("error", "No resource was found for this request.")
      ctx
        .response()
        .setStatusCode(404)
        .putHeader("Content-Type", "application/json")
        .end(error.encode())
    }
}
