package io.andrsn.matrix

import com.dslplatform.json.DslJson
import io.andrsn.matrix.dto.LoginRequest
import io.andrsn.matrix.dto.LoginResponse
import io.andrsn.matrix.dto.MatrixRequest
import io.andrsn.matrix.dto.RegisterRequest
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.RegisterSuccessResponse
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object MatrixTestUtils {

  @PublishedApi
  internal val json = DslJson<Any>()

  /**
   * Simulates a Matrix HTTP request and deserializes the response.
   *
   * @param matrix The Matrix instance to test
   * @param method HTTP method (GET, POST, etc.)
   * @param path Request path
   * @param body Optional request body (will be serialized to JSON)
   * @param accessToken Optional access token for authenticated requests
   * @param expectedStatus Expected HTTP status code (default: 200)
   * @return TestResponse containing status code and deserialized data
   */
  inline fun <reified T> simulateRequest(
    matrix: Matrix,
    method: String,
    path: String,
    body: Any? = null,
    accessToken: String? = null,
    expectedStatus: Int = 200,
  ): TestResponse<T> {
    var actualStatus = 0
    var responseData: T? = null
    matrix.handleEvent(
      MatrixRequest().apply<MatrixRequest> {
        this.method = method
        this.path = path
        this.accessToken = accessToken
        requestBody = if (body != null) {
          val out = ByteArrayOutputStream()
          json.serialize(body, out)
          out.toByteArray()
        } else {
          ByteArray(0)
        }
        responseStream = ByteArrayOutputStream()
        finishResponse = { statusCode: Int ->
          actualStatus = statusCode
          if (statusCode == expectedStatus) {
            responseData = json.deserialize<T>(
              T::class.java,
              ByteArrayInputStream(responseStream.toByteArray()),
            )
          }
        }
      },
    )
    assertThat(actualStatus)
      .withFailMessage(
        "Expected status $expectedStatus but got $actualStatus",
      ).isEqualTo(expectedStatus)
    assertThat(responseData).isNotNull
    val result = TestResponse(actualStatus, responseData)

    return TestResponse(result.statusCode, result.data!!)
  }

  data class TestResponse<T>(
    val statusCode: Int,
    val data: T,
  )

  /**
   * Gets an authentication session for registration.
   */
  fun getAuthSession(matrix: Matrix): String {
    val request = RegisterRequest(
      username = null,
      password = null,
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = null,
    )

    val response = simulateRequest<RegisterResponse>(
      matrix = matrix,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
      expectedStatus = 401,
    )

    return response.data.session
  }

  /**
   * Registers a new user with the given username and password.
   */
  fun registerUser(
    matrix: Matrix,
    username: String,
    password: String,
    inhibitLogin: Boolean? = null,
  ): RegisterSuccessResponse {
    val session = getAuthSession(matrix)
    return simulateRequest<RegisterSuccessResponse>(
      matrix = matrix,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = RegisterRequest(
        username = username,
        password = password,
        deviceId = null,
        initialDeviceDisplayName = null,
        inhibitLogin = inhibitLogin,
        authentication = RegisterRequest.Authentication(
          type = "m.login.dummy",
          session = session,
        ),
      ),
      expectedStatus = 200,
    ).data
  }

  /**
   * Logs in a user with the given username and password.
   */
  fun loginUser(
    matrix: Matrix,
    username: String,
    password: String,
  ): LoginResponse =
    simulateRequest<LoginResponse>(
      matrix = matrix,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = LoginRequest(
        type = "m.login.password",
        identifier = LoginRequest.Identifier(
          type = "m.id.user",
          user = username,
        ),
        password = password,
        deviceId = null,
        initialDeviceDisplayName = null,
      ),
      expectedStatus = 200,
    ).data
}
