package io.andrsn.matrix

import com.dslplatform.json.DslJson
import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.MatrixRequest
import io.andrsn.matrix.dto.VersionsResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MatrixTest {

  private val json = DslJson<Any>()
  private val sut = Matrix()

  @Test
  fun `it should support v1_11`() {
    val result =
      sut.simulateRequest<VersionsResponse>("/_matrix/client/versions")

    assertThat(result.statusCode).isEqualTo(200)
    assertThat(result.data.versions).contains("v1.11")
  }

  @Test
  fun `it should give supported login types`() {
    val result =
      sut.simulateRequest<LoginTypesResponse>("/_matrix/client/v3/login")

    assertThat(result.statusCode).isEqualTo(200)
    assertThat(result.data.flows)
      .contains(LoginTypesResponse.Flow("m.login.password"))
  }

  private inline fun <reified T> Matrix.simulateRequest(
    method: String,
  ): Response<T> {
    var responseData: T? = null

    this.handleEvent(
      MatrixRequest().apply {
        this.method = method
        this.responseStream = ByteArrayOutputStream()
        finishResponse = { statusCode: Int ->
          assertThat(statusCode).isEqualTo(200)
          val inputStream = ByteArrayInputStream(responseStream.toByteArray())
          responseData = json.deserialize(T::class.java, inputStream)
        }
      },
    )

    assertThat(responseData).isNotNull
    return Response(200, responseData!!)
  }

  private data class Response<T>(
    val statusCode: Int,
    val data: T,
  )
}
