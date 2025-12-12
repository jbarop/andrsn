package io.andrsn.matrix

import io.andrsn.matrix.MatrixTestUtils.getAuthSession
import io.andrsn.matrix.MatrixTestUtils.registerUser
import io.andrsn.matrix.MatrixTestUtils.simulateRequest
import io.andrsn.matrix.dto.ErrorResponse
import io.andrsn.matrix.dto.RegisterRequest
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.RegisterSuccessResponse
import io.andrsn.matrix.dto.UsernameAvailableResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterIntegrationTest {

  private lateinit var sut: Matrix

  @BeforeEach
  fun setup() {
    sut = Matrix("localhost")
  }

  @Test
  fun `should check username availability`() {
    val response = simulateRequest<UsernameAvailableResponse>(
      matrix = sut,
      method = "GET",
      path = "/_matrix/client/v3/register/available",
    )

    assertThat(response.statusCode).isEqualTo(200)
    assertThat(response.data.available).isTrue()
  }

  @Test
  fun `full registration flow should succeed`() {
    val initialRequest = RegisterRequest(
      username = "testuser",
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = null,
    )

    val uiaResponse = simulateRequest<RegisterResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = initialRequest,
    )

    assertThat(uiaResponse.statusCode).isEqualTo(401)
    assertThat(uiaResponse.data.session).isNotBlank()
    assertThat(uiaResponse.data.flows).hasSize(1)
    assertThat(
      uiaResponse.data.flows
        .first()
        .stages,
    ).containsExactly("m.login.dummy")

    val sessionId = uiaResponse.data.session
    val completeRequest = RegisterRequest(
      username = "testuser",
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = sessionId,
      ),
    )

    val successResponse = simulateRequest<RegisterSuccessResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = completeRequest,
    )

    assertThat(successResponse.statusCode).isEqualTo(200)
    assertThat(successResponse.data.userId).isEqualTo("@testuser:localhost")
    assertThat(successResponse.data.homeServer).isEqualTo("localhost")
    assertThat(successResponse.data.accessToken).hasSize(48)
    assertThat(successResponse.data.deviceId).hasSize(48)
  }

  @Test
  fun `should reject registration without username`() {
    val session = getAuthSession(sut)

    val request = RegisterRequest(
      username = null,
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = session,
      ),
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_MISSING_PARAM")
    assertThat(response.data.error).contains("username")
  }

  @Test
  fun `should reject registration without password`() {
    val session = getAuthSession(sut)

    val request = RegisterRequest(
      username = "testuser",
      password = null,
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = session,
      ),
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_MISSING_PARAM")
    assertThat(response.data.error).contains("password")
  }

  @Test
  fun `should reject duplicate username`() {
    registerUser(sut, "testuser", "password123")

    val session = getAuthSession(sut)
    val request = RegisterRequest(
      username = "testuser",
      password = "different_password",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = session,
      ),
    )

    val response = simulateRequest<ErrorResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
    )

    assertThat(response.statusCode).isEqualTo(400)
    assertThat(response.data.errcode).isEqualTo("M_USER_IN_USE")
    assertThat(response.data.error).contains("already taken")
  }

  @Test
  fun `should handle multiple registrations`() {
    val user1 = registerUser(sut, "user1", "password1")
    val user2 = registerUser(sut, "user2", "password2")
    val user3 = registerUser(sut, "user3", "password3")

    assertThat(user1.userId).isEqualTo("@user1:localhost")
    assertThat(user2.userId).isEqualTo("@user2:localhost")
    assertThat(user3.userId).isEqualTo("@user3:localhost")

    assertThat(user1.accessToken).isNotEqualTo(user2.accessToken)
    assertThat(user2.accessToken).isNotEqualTo(user3.accessToken)
  }

  @Test
  fun `should reject registration with invalid session`() {
    val request = RegisterRequest(
      username = "testuser",
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = "invalid_session_id",
      ),
    )

    val response = simulateRequest<RegisterResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
    )

    assertThat(response.statusCode).isEqualTo(401)
    assertThat(response.data.session).isNotBlank()
  }

  @Test
  fun `should generate unique tokens for same user registrations`() {
    val user1 = registerUser(sut, "user1", "password")
    val user2 = registerUser(sut, "user2", "password")

    assertThat(user1.accessToken).hasSize(48)
    assertThat(user2.accessToken).hasSize(48)
    assertThat(user1.accessToken).isNotEqualTo(user2.accessToken)

    assertThat(user1.deviceId).hasSize(48)
    assertThat(user2.deviceId).hasSize(48)
    assertThat(user1.deviceId).isNotEqualTo(user2.deviceId)
  }

  @Test
  fun `should verify password is hashed`() {
    val user = registerUser(sut, "testuser", "mypassword")

    assertThat(user.accessToken).doesNotContain("mypassword")
    assertThat(user.userId).doesNotContain("mypassword")
  }

  @Test
  fun `should not return access token and device ID when inhibitLogin=null`() {
    val session = getAuthSession(sut)

    val request = RegisterRequest(
      username = "testuser",
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = null,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = session,
      ),
    )

    val response = simulateRequest<RegisterSuccessResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
    )

    assertThat(response.statusCode).isEqualTo(200)
    assertThat(response.data.accessToken).isNotNull()
    assertThat(response.data.deviceId).isNotNull()
  }

  @Test
  fun `should not return access token and device ID when inhibitLogin=false`() {
    val session = getAuthSession(sut)

    val request = RegisterRequest(
      username = "testuser",
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = false,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = session,
      ),
    )

    val response = simulateRequest<RegisterSuccessResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
    )

    assertThat(response.statusCode).isEqualTo(200)
    assertThat(response.data.accessToken).isNotNull()
    assertThat(response.data.deviceId).isNotNull()
  }

  @Test
  fun `should not return access token and device ID when inhibitLogin=true`() {
    val session = getAuthSession(sut)

    val request = RegisterRequest(
      username = "testuser",
      password = "password123",
      deviceId = null,
      initialDeviceDisplayName = null,
      inhibitLogin = true,
      authentication = RegisterRequest.Authentication(
        type = "m.login.dummy",
        session = session,
      ),
    )

    val response = simulateRequest<RegisterSuccessResponse>(
      matrix = sut,
      method = "POST",
      path = "/_matrix/client/v3/register",
      body = request,
    )

    assertThat(response.statusCode).isEqualTo(200)
    assertThat(response.data.accessToken).isNull()
    assertThat(response.data.deviceId).isNull()
  }
}
