package io.andrsn.matrix

import com.dslplatform.json.DslJson
import io.andrsn.matrix.dto.ErrorResponse
import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.MatrixRequest
import io.andrsn.matrix.dto.MatrixRequest.Method.CLIENT_GET_LOGIN
import io.andrsn.matrix.dto.MatrixRequest.Method.CLIENT_GET_VERSIONS
import io.andrsn.matrix.dto.MatrixRequest.Method.CLIENT_POST_REGISTER
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.VersionsResponse

class Matrix {

  private val json = DslJson<Any>()

  fun handleEvent(event: MatrixRequest) =
    try {
      when (event.method) {
        CLIENT_GET_VERSIONS -> getSupportedVersions(event)
        CLIENT_GET_LOGIN -> getLoginTypes(event)
        CLIENT_POST_REGISTER -> register(event)
        null -> {
          event.sendResponse(
            400,
            ErrorResponse(
              errcode = "M_NOT_FOUND",
              error = "No resource was found for this request.",
            ),
          )
        }
      }
    } catch (e: Exception) {
      println("Error handling event: ${e.localizedMessage}")
      e.printStackTrace()
      event.finishResponse(500)
    }

  private fun getSupportedVersions(event: MatrixRequest) =
    event.sendResponse(
      statusCode = 200,
      data =
        VersionsResponse(
          unstableFeatures = emptyMap(),
          versions =
            listOf(
              "v1.1",
              "v1.2",
              "v1.3",
              "v1.4",
              "v1.5",
              "v1.6",
              "v1.7",
              "v1.8",
              "v1.9",
              "v1.10",
              "v1.11",
            ),
        ),
    )

  private fun getLoginTypes(request: MatrixRequest) =
    request.sendResponse(
      statusCode = 200,
      data =
        LoginTypesResponse(
          flows =
            setOf(
              LoginTypesResponse.Flow(type = "m.login.password"),
            ),
        ),
    )

  private fun register(request: MatrixRequest) =
    request.sendResponse(
      statusCode = 401,
      data =
        RegisterResponse(
          session = SessionGenerator.generateSessionId(),
          flows =
            setOf(
              RegisterResponse.Flow(
                listOf("m.login.email.identity"),
              ),
            ),
        ),
    )

  private fun MatrixRequest.sendResponse(
    statusCode: Int,
    data: Any,
  ) {
    json.serialize(data, responseStream)
    this.finishResponse(statusCode)
  }
}
