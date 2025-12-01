package io.andrsn.matrix

import com.dslplatform.json.DslJson
import io.andrsn.matrix.dto.MatrixRequest
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
   * @param expectedStatus Expected HTTP status code (default: 200)
   * @return TestResponse containing status code and deserialized data
   */
  inline fun <reified T> simulateRequest(
    matrix: Matrix,
    method: String,
    path: String,
    body: Any? = null,
    expectedStatus: Int = 200,
  ): TestResponse<T> {
    var actualStatus = 0
    var responseData: T? = null
    matrix.handleEvent(
      MatrixRequest().apply<MatrixRequest> {
        this.method = method
        this.path = path
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
}
