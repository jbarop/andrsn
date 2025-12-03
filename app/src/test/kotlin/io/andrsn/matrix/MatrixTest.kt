package io.andrsn.matrix

import io.andrsn.matrix.MatrixTestUtils.loginUser
import io.andrsn.matrix.MatrixTestUtils.registerUser
import io.andrsn.matrix.MatrixTestUtils.simulateRequest
import io.andrsn.matrix.dto.ErrorResponse
import io.andrsn.matrix.dto.VersionsResponse
import io.andrsn.matrix.dto.WhoAmIResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MatrixTest {

  private lateinit var sut: Matrix

  @BeforeEach
  fun setup() {
    sut = Matrix()
  }

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
  fun `whoami should return user info with valid token`() {
    registerUser(sut, "testuser", "password123")
    val loginResponse = loginUser(sut, "testuser", "password123")

    val response = simulateRequest<WhoAmIResponse>(
      matrix = sut,
      method = "GET",
      path = "/_matrix/client/v3/account/whoami",
      accessToken = loginResponse.accessToken,
    )

    assertThat(response.statusCode).isEqualTo(200)
    assertThat(response.data.userId).isEqualTo("@testuser:localhost")
    assertThat(response.data.deviceId).isEqualTo(loginResponse.deviceId)
  }

  @Test
  fun `whoami should reject request without token`() {
    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "GET",
      path = "/_matrix/client/v3/account/whoami",
      accessToken = null,
      expectedStatus = 400,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_MISSING_TOKEN")
    assertThat(response.data.error).contains("Missing access token")
  }

  @Test
  fun `whoami should reject request with invalid token`() {
    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "GET",
      path = "/_matrix/client/v3/account/whoami",
      accessToken = "invalid_token_xyz",
      expectedStatus = 401,
    )

    assertThat(response.statusCode).isEqualTo(401)
    assertThat(response.data.errcode).isEqualTo("M_UNKNOWN_TOKEN")
    assertThat(response.data.error).contains("Unrecognised access token")
  }
}
