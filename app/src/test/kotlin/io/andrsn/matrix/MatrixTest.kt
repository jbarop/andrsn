package io.andrsn.matrix

import io.andrsn.matrix.dto.LoginTypesResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MatrixTest {

  private val sut = Matrix()

  @Test
  fun `it should support v1_11`() {
    assertThat(sut.getSupportedVersions().versions).contains("v1.11")
  }

  @Test
  fun `it should give supported login types`() {
    assertThat(sut.getLoginTypes().flows).contains(LoginTypesResponse.Flow("m.login.password"))
  }

}
