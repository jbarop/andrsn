package io.andrsn.matrix

import io.andrsn.matrix.MatrixTestUtils.simulateRequest
import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.VersionsResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MatrixTest {

  private val sut = Matrix()

  @Test
  fun `it should support v1_11`() {
    val result = simulateRequest<VersionsResponse>(
      matrix = sut,
      method = "GET",
      path = "/_matrix/client/versions",
    )

    assertThat(result.statusCode).isEqualTo(200)
    assertThat(result.data.versions).contains("v1.11")
  }

  @Test
  fun `it should give supported login types`() {
    val result = simulateRequest<LoginTypesResponse>(
      matrix = sut,
      method = "GET",
      path = "/_matrix/client/v3/login",
    )

    assertThat(result.statusCode).isEqualTo(200)
    assertThat(result.data.flows)
      .contains(LoginTypesResponse.Flow("m.login.password"))
  }
}
