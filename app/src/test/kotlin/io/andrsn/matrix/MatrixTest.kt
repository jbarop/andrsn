package io.andrsn.matrix

import io.andrsn.matrix.dto.LoginTypesResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MatrixTest {

  private val sut = Matrix()

  @Test
  fun `it should support v1_11`() {
    val result = sut.getSupportedVersions()

    assertThat(result.statusCode).isEqualTo(200)
    assertThat(result.data.versions).contains("v1.11")
  }

  @Test
  fun `it should give supported login types`() {
    val result = sut.getLoginTypes()

    assertThat(result.statusCode).isEqualTo(200)
    assertThat(result.data.flows)
      .contains(LoginTypesResponse.Flow("m.login.password"))
  }
}
