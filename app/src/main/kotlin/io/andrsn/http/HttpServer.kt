package io.andrsn.http

import com.dslplatform.json.DslJson
import com.dslplatform.json.ParsingException
import io.andrsn.matrix.Matrix
import io.andrsn.matrix.dto.ErrorResponse
import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.MatrixResponse
import io.andrsn.matrix.dto.RegisterRequest
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.VersionsResponse
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
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
 * It exposes Matrix API endpoints and handles JSON (de)serialization,
 * error responses, and CORS. Business logic is delegated to a [Matrix]
 * instance.
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

      route().handler { ctx ->
        ctx
          .sendResponse(
            statusCode = 404,
            data =
              ErrorResponse(
                errcode = "M_NOT_FOUND",
                error = "No resource was found for this request.",
              ),
          )
      }
    }

  private fun <T> RoutingContext.sendResponse(
    statusCode: Int,
    data: T,
  ) {
    val baos = ByteArrayOutputStream()
    json.serialize(data, baos)
    response()
      .setStatusCode(statusCode)
      .putHeader(CONTENT_TYPE, APPLICATION_JSON)
      .end(Buffer.buffer(baos.toByteArray()))
  }

  private inline fun <reified RequestDto, ResponseDto> Route.handler(
    crossinline block: (dto: RequestDto) -> MatrixResponse<ResponseDto>,
  ) {
    this.handler { ctx ->
      ctx.request().bodyHandler { body ->
        val requestDto =
          try {
            if (RequestDto::class.java == Unit.javaClass) {
              Unit as RequestDto
            } else {
              json.deserialize(
                RequestDto::class.java,
                body.bytes,
                body.bytes.size,
              )
            }
          } catch (e: Exception) {
            when (e) {
              is ParsingException -> {
                ctx.sendResponse(
                  statusCode = 400,
                  data =
                    ErrorResponse(
                      errcode = "M_NOT_JSON",
                      error = "Invalid request: ${e.localizedMessage}",
                    ),
                )
              }

              is NullPointerException -> {
                ctx.sendResponse(
                  statusCode = 400,
                  data =
                    ErrorResponse(
                      errcode = "M_BAD_JSON",
                      error = "Invalid request: ${e.localizedMessage}",
                    ),
                )
              }

              else -> {
                ctx.sendResponse(
                  statusCode = 500,
                  data =
                    ErrorResponse(
                      errcode = "M_UNKNOWN",
                      error = e.localizedMessage,
                    ),
                )
              }
            }
            return@bodyHandler
          }

        val matrixResponse = block(requestDto)
        ctx
          .sendResponse(
            statusCode = matrixResponse.statusCode,
            data = matrixResponse.data,
          )
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
