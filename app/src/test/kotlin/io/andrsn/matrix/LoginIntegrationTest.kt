package io.andrsn.matrix

import io.andrsn.matrix.MatrixTestUtils.loginUser
import io.andrsn.matrix.MatrixTestUtils.registerUser
import io.andrsn.matrix.MatrixTestUtils.simulateRequest
import io.andrsn.matrix.dto.ErrorResponse
import io.andrsn.matrix.dto.LoginRequest
import io.andrsn.matrix.dto.LoginResponse
import io.andrsn.matrix.dto.LoginTypesResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginIntegrationTest {

  private lateinit var sut: Matrix

  @BeforeEach
  fun setup() {
    sut = Matrix()
  }

  @Test
  fun `should return supported login types`() {
    val response = simulateRequest<LoginTypesResponse>(
      matrix = sut,
      method = "GET",
      path = "/_matrix/client/v3/login",
    )

    assertThat(response.statusCode).isEqualTo(200)
    assertThat(response.data.flows).hasSize(1)
    assertThat(response.data.flows)
      .contains(LoginTypesResponse.Flow("m.login.password"))
  }

  @Test
  fun `should login successfully with correct credentials`() {
    registerUser(sut, "testuser", "password123")

    val loginRequest = LoginRequest(
      type = "m.login.password",
      identifier = LoginRequest.Identifier(
        type = "m.id.user",
        user = "testuser",
      ),
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
    )

    val response = simulateRequest<LoginResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = loginRequest,
    )

    assertThat(response.statusCode).isEqualTo(200)
    assertThat(response.data.userId).isEqualTo("@testuser:localhost")
    assertThat(response.data.homeServer).isEqualTo("localhost")
    assertThat(response.data.accessToken).hasSize(48)
    assertThat(response.data.deviceId).hasSize(48)
  }

  @Test
  fun `should reject login with incorrect password`() {
    registerUser(sut, "testuser", "correctPassword")

    val loginRequest = LoginRequest(
      type = "m.login.password",
      identifier = LoginRequest.Identifier(
        type = "m.id.user",
        user = "testuser",
      ),
      password = "wrongPassword",
      deviceId = null,
      initialDeviceDisplayName = null,
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = loginRequest,
    )

    assertThat(response.statusCode).isEqualTo(401)
    assertThat(response.data.errcode).isEqualTo("M_UNAUTHORIZED")
    assertThat(response.data.error).contains("Invalid username or password")
  }

  @Test
  fun `should reject login with non-existent user`() {
    val loginRequest = LoginRequest(
      type = "m.login.password",
      identifier = LoginRequest.Identifier(
        type = "m.id.user",
        user = "nonexistent",
      ),
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = loginRequest,
    )

    assertThat(response.statusCode).isEqualTo(401)
    assertThat(response.data.errcode).isEqualTo("M_UNAUTHORIZED")
    assertThat(response.data.error).contains("Invalid username or password")
  }

  @Test
  fun `should reject login without username`() {
    val loginRequest = LoginRequest(
      type = "m.login.password",
      identifier = LoginRequest.Identifier(
        type = "m.id.user",
        user = null,
      ),
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = loginRequest,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_MISSING_PARAM")
    assertThat(response.data.error).contains("username")
  }

  @Test
  fun `should reject login without password`() {
    val loginRequest = LoginRequest(
      type = "m.login.password",
      identifier = LoginRequest.Identifier(
        type = "m.id.user",
        user = "testuser",
      ),
      password = null,
      deviceId = null,
      initialDeviceDisplayName = null,
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = loginRequest,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_MISSING_PARAM")
    assertThat(response.data.error).contains("password")
  }

  @Test
  fun `should reject login with unsupported login type`() {
    val loginRequest = LoginRequest(
      type = "m.login.token",
      identifier = LoginRequest.Identifier(
        type = "m.id.user",
        user = "testuser",
      ),
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = loginRequest,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_UNKNOWN")
    assertThat(response.data.error).contains("Bad login type")
  }

  @Test
  fun `should reject login with unsupported identifier type`() {
    val loginRequest = LoginRequest(
      type = "m.login.password",
      identifier = LoginRequest.Identifier(
        type = "m.id.thirdparty",
        user = "testuser",
      ),
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/login",
      body = loginRequest,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_UNKNOWN")
    assertThat(response.data.error).contains("Bad login type")
  }

  @Test
  fun `should generate unique tokens for same user login`() {
    registerUser(sut, "testuser", "password123")

    val login1 = loginUser(sut, "testuser", "password123")
    val login2 = loginUser(sut, "testuser", "password123")

    assertThat(login1.accessToken).hasSize(48)
    assertThat(login2.accessToken).hasSize(48)
    assertThat(login1.accessToken).isNotEqualTo(login2.accessToken)

    assertThat(login1.deviceId).hasSize(48)
    assertThat(login2.deviceId).hasSize(48)
    assertThat(login1.deviceId).isNotEqualTo(login2.deviceId)
  }
}
