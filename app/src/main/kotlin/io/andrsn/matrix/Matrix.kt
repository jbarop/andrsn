package io.andrsn.matrix

import com.dslplatform.json.DslJson
import io.andrsn.matrix.dto.ErrorResponse
import io.andrsn.matrix.dto.LoginRequest
import io.andrsn.matrix.dto.LoginResponse
import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.MatrixRequest
import io.andrsn.matrix.dto.RegisterRequest
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.RegisterSuccessResponse
import io.andrsn.matrix.dto.UsernameAvailableResponse
import io.andrsn.matrix.dto.VersionsResponse
import io.andrsn.matrix.dto.WhoAmIResponse
import java.io.ByteArrayInputStream

class Matrix {

  private val json = DslJson<Any>()
  private val authenticationSessions = AuthenticationSessions()
  private val userSessions = UserSessions()
  private val passwordHasher = PasswordHasher()
  private val userStore = UserStore()

  fun handleEvent(event: MatrixRequest) =
    with(event) {
      try {
        when {
          (method == "GET" && path == "/_matrix/client/versions") ->
            getSupportedVersions(event)

          (method == "GET" && path == "/_matrix/client/v3/login") ->
            getLoginTypes(event)

          (method == "POST" && path == "/_matrix/client/v3/login") ->
            login(event)

          (method == "POST" && path == "/_matrix/client/v3/logout") ->
            logout(event)

          (method == "POST" && path == "/_matrix/client/v3/logout/all") ->
            logoutAll(event)

          (method == "GET" && path == "/_matrix/client/v3/register/available")
          -> isUserNameAvailable(event)

          (method == "POST" && path == "/_matrix/client/v3/register") ->
            register(event)

          (method == "GET" && path == "/_matrix/client/v3/account/whoami") ->
            whoAmI(event)

          else -> {
            event.sendResponse(
              statusCode = 404,
              data = ErrorResponse(
                errcode = "M_NOT_FOUND",
                error = "No resource was found for this request.",
              ),
            )
          }
        }
      } catch (e: Exception) {
        println("Error handling event: ${e.localizedMessage}")
        e.printStackTrace()
        event.sendResponse(
          statusCode = 500,
          data = ErrorResponse(
            errcode = "M_UNKNOWN",
            error = "Internal server error.",
          ),
        )
      }
    }

  private fun getSupportedVersions(event: MatrixRequest) =
    event.sendResponse(
      statusCode = 200,
      data = VersionsResponse(
        unstableFeatures = emptyMap(),
        versions = listOf(
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
      data = LoginTypesResponse(
        flows = setOf(
          LoginTypesResponse.Flow(type = "m.login.password"),
        ),
      ),
    )

  private fun login(request: MatrixRequest) {
    val loginReq = json.deserialize(
      LoginRequest::class.java,
      ByteArrayInputStream(request.requestBody),
    ) ?: return request.sendResponse(
      statusCode = 400,
      data = ErrorResponse(
        errcode = "M_BAD_JSON",
        error = "Could not deserialize login request.",
      ),
    )

    if (loginReq.type != "m.login.password") {
      return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_UNKNOWN",
          error = "Bad login type.",
        ),
      )
    }

    if (loginReq.identifier?.type != "m.id.user") {
      return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_UNKNOWN",
          error = "Bad login type.",
        ),
      )
    }

    val username = loginReq.identifier.user
      ?: return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_PARAM",
          error = "Missing username.",
        ),
      )
    val password = loginReq.password
      ?: return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_PARAM",
          error = "Missing password.",
        ),
      )
    val user = userStore.getUser(username)
    val valid = passwordHasher.verifyPassword(password, user?.passwordHash)

    if (!valid || user == null) {
      return request.sendResponse(
        statusCode = 401,
        data = ErrorResponse(
          errcode = "M_UNAUTHORIZED",
          error = "Invalid username or password.",
        ),
      )
    }

    val newSession = userSessions.create(
      userId = "@${user.username}:localhost",
      deviceId = TokenGenerator.generate(),
    )

    request.sendResponse(
      statusCode = 200,
      data = LoginResponse(
        userId = newSession.userId,
        homeServer = "localhost",
        accessToken = newSession.accessToken,
        deviceId = newSession.deviceId,
      ),
    )
  }

  private fun logout(request: MatrixRequest) {
    if (request.accessToken == null) {
      return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_TOKEN",
          error = "Missing access token.",
        ),
      )
    }

    userSessions.find(request.accessToken!!)
      ?: return request.sendResponse(
        statusCode = 401,
        data = ErrorResponse(
          errcode = "M_UNKNOWN_TOKEN",
          error = "Unrecognised access token.",
        ),
      )

    userSessions.delete(request.accessToken!!)

    request.sendResponse(
      statusCode = 200,
      data = emptyMap<String, Any>(),
    )
  }

  private fun logoutAll(request: MatrixRequest) {
    if (request.accessToken == null) {
      return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_TOKEN",
          error = "Missing access token.",
        ),
      )
    }

    val session = userSessions.find(request.accessToken!!)
      ?: return request.sendResponse(
        statusCode = 401,
        data = ErrorResponse(
          errcode = "M_UNKNOWN_TOKEN",
          error = "Unrecognised access token.",
        ),
      )

    userSessions.deleteAllForUser(session.userId)

    request.sendResponse(
      statusCode = 200,
      data = emptyMap<String, Any>(),
    )
  }

  private fun isUserNameAvailable(request: MatrixRequest) =
    request.sendResponse(
      statusCode = 200,
      data = UsernameAvailableResponse(
        available = true,
      ),
    )

  private fun register(request: MatrixRequest) {
    val registerReq = json.deserialize(
      RegisterRequest::class.java,
      ByteArrayInputStream(request.requestBody),
    ) ?: return request.sendResponse(
      statusCode = 400,
      data = ErrorResponse(
        errcode = "M_BAD_JSON",
        error = "Could not deserialize register request.",
      ),
    )

    val authSession = if (registerReq.authentication?.session != null) {
      authenticationSessions.find(registerReq.authentication.session)
    } else {
      null
    }

    if (authSession == null) {
      val newAuthSession = authenticationSessions.create()
      return request.sendResponse(
        statusCode = 401,
        data = RegisterResponse(
          session = newAuthSession.sessionId,
          flows = setOf(
            RegisterResponse.Flow(
              stages = listOf("m.login.dummy"),
            ),
          ),
        ),
      )
    }

    val username = registerReq.username
      ?: return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_PARAM",
          error = "Missing username.",
        ),
      )
    val password = registerReq.password
      ?: return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_PARAM",
          error = "Missing password.",
        ),
      )

    if (userStore.usernameExists(username)) {
      return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_USER_IN_USE",
          error = "Username already taken.",
        ),
      )
    }

    val user = User(
      username = username,
      passwordHash = passwordHasher.hashPassword(password),
    )
    userStore.addUser(user)

    if (registerReq.inhibitLogin == true) {
      request.sendResponse(
        200,
        RegisterSuccessResponse(
          userId = "@${user.username}:localhost",
          homeServer = "localhost",
          accessToken = null,
          deviceId = null,
        ),
      )
    } else {
      val newSession = userSessions.create(
        userId = "@${user.username}:localhost",
        deviceId = TokenGenerator.generate(),
      )
      request.sendResponse(
        statusCode = 200,
        data = RegisterSuccessResponse(
          userId = newSession.userId,
          homeServer = "localhost",
          accessToken = newSession.accessToken,
          deviceId = newSession.deviceId,
        ),
      )
    }

    authenticationSessions.remove(authSession.sessionId)
  }

  private fun whoAmI(request: MatrixRequest) {
    if (request.accessToken == null) {
      return request.sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_TOKEN",
          error = "Missing access token.",
        ),
      )
    }

    val session = userSessions.find(request.accessToken!!)
      ?: return request.sendResponse(
        statusCode = 401,
        data = ErrorResponse(
          errcode = "M_UNKNOWN_TOKEN",
          error = "Unrecognised access token.",
        ),
      )

    request.sendResponse(
      statusCode = 200,
      data = WhoAmIResponse(
        userId = session.userId,
        deviceId = session.deviceId,
      ),
    )
  }

  private fun MatrixRequest.sendResponse(
    statusCode: Int,
    data: Any,
  ) {
    json.serialize(data, responseStream)
    this.finishResponse(statusCode)
  }
}
