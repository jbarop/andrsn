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
import io.andrsn.matrix.dto.SyncResponse
import io.andrsn.matrix.dto.SyncRoom
import io.andrsn.matrix.dto.SyncRoomEvent
import io.andrsn.matrix.dto.SyncRoomState
import io.andrsn.matrix.dto.SyncRoomTimeline
import io.andrsn.matrix.dto.SyncRooms
import io.andrsn.matrix.dto.UsernameAvailableResponse
import io.andrsn.matrix.dto.VersionsResponse
import io.andrsn.matrix.dto.WhoAmIResponse
import java.io.ByteArrayInputStream
import java.time.LocalDate

class Matrix {

  private val json = DslJson(DslJson.Settings<Any>().includeServiceLoader())
  private val authenticationSessions = AuthenticationSessions()
  private val userSessions = UserSessions()
  private val passwordHasher = PasswordHasher()
  private val userStore = UserStore()

  init {
    // Create test user for testing
    userStore.addUser(
      User(
        username = "test",
        passwordHash = passwordHasher.hashPassword("test"),
      ),
    )
    println("Test user 'test:test' created")
  }

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

          (method == "GET" && path == "/_matrix/client/v3/sync") ->
            sync(event)

          (method == "GET" && path == "/_matrix/client/v3/pushrules/") ->
            getPushRules(event)

          (
            method == "POST" &&
              path.startsWith("/_matrix/client/v3/user/") &&
              path.endsWith("/filter")
          ) ->
            createFilter(event)

          (method == "POST" && path == "/_matrix/client/v3/keys/upload") ->
            keysUpload(event)

          (method == "POST" && path == "/_matrix/client/v3/keys/query") ->
            keysQuery(event)

          (method == "POST" && path == "/_matrix/client/v3/keys/claim") ->
            keysClaim(event)

          (method == "GET" && path == "/_matrix/client/v3/capabilities") ->
            getCapabilities(event)

          (
            path.startsWith("/_matrix/client/v3/user/") &&
              path.contains("/account_data/")
          ) ->
            handleAccountData(event)

          (method == "GET" && path == "/_matrix/client/v3/thirdparty/protocols")
          -> thirdPartyProtocols(event)

          (method == "GET" && path == "/_matrix/client/v3/room_keys/version") ->
            roomKeysVersion(event)

          else -> {
            if (!path.contains("client/unstable")) {
              println("Unknown request: $method $path")
            }
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
    request.getAccessToken() ?: return

    userSessions.delete(request.accessToken!!)

    request.sendResponse(
      statusCode = 200,
      data = emptyMap<String, Any>(),
    )
  }

  private fun logoutAll(request: MatrixRequest) {
    val session = request.getAccessToken() ?: return

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
    val session = request.getAccessToken() ?: return

    request.sendResponse(
      statusCode = 200,
      data = WhoAmIResponse(
        userId = session.userId,
        deviceId = session.deviceId,
      ),
    )
  }

  private fun sync(request: MatrixRequest) {
    val session = request.getAccessToken() ?: return

    val roomId = "!testroom:localhost"
    val roomName = "Test Room"
    val roomCreated = LocalDate
      .of(2025, 1, 1)
      .atStartOfDay()
      .toInstant(java.time.ZoneOffset.UTC)
      .toEpochMilli()

    request.sendResponse(
      statusCode = 200,
      data = SyncResponse(
        nextBatch = "s1",
        rooms = SyncRooms(
          join = mapOf(
            roomId to SyncRoom(
              timeline = SyncRoomTimeline(
                events = emptyList(),
                limited = false,
                prevBatch = "t0",
              ),
              state = SyncRoomState(
                events = listOf(
                  SyncRoomEvent(
                    type = "m.room.create",
                    stateKey = "",
                    content = mapOf(
                      "creator" to session.userId,
                      "roomVersion" to "1",
                    ),
                    sender = session.userId,
                    originServerTs = roomCreated,
                    eventId = $$"$room_created:localhost",
                  ),
                  SyncRoomEvent(
                    type = "m.room.member",
                    stateKey = session.userId,
                    content = mapOf(
                      "membership" to "join",
                      "displayname" to session.userId,
                    ),
                    sender = session.userId,
                    originServerTs = roomCreated,
                    eventId = $$"$member:localhost",
                  ),
                  SyncRoomEvent(
                    type = "m.room.name",
                    stateKey = "",
                    content = mapOf(
                      "name" to roomName,
                    ),
                    sender = session.userId,
                    originServerTs = roomCreated,
                    eventId = $$"$name:localhost",
                  ),
                ),
              ),
            ),
          ),
          invite = emptyMap(),
          leave = emptyMap(),
        ),
      ),
    )
  }

  private fun createFilter(request: MatrixRequest) {
    request.getAccessToken() ?: return

    request.sendRawJsonResponse(
      statusCode = 200,
      json = """{"filter_id":"0"}""",
    )
  }

  private fun keysUpload(request: MatrixRequest) {
    request.getAccessToken() ?: return

    request.sendRawJsonResponse(
      statusCode = 200,
      json = """{"one_time_key_counts":{}}""",
    )
  }

  private fun keysQuery(request: MatrixRequest) {
    request.getAccessToken() ?: return

    request.sendRawJsonResponse(
      statusCode = 200,
      json = """{"device_keys":{}}""",
    )
  }

  private fun keysClaim(request: MatrixRequest) {
    request.getAccessToken() ?: return

    request.sendRawJsonResponse(
      statusCode = 200,
      json = """{"one_time_keys":{}}""",
    )
  }

  private fun getCapabilities(request: MatrixRequest) {
    request.getAccessToken() ?: return

    request.sendRawJsonResponse(
      statusCode = 200,
      json =
        """
        {
          "capabilities": {
            "m.change_password": {"enabled": false},
            "m.room_versions": {
              "available": {"1": "stable"},
              "default": "1"
            }
          }
        }
        """.trimIndent(),
    )
  }

  private fun handleAccountData(request: MatrixRequest) {
    request.getAccessToken() ?: return

    when (request.method) {
      "PUT" -> {
        request.sendRawJsonResponse(
          statusCode = 200,
          json = """{}""",
        )
      }

      "GET" -> {
        request.sendResponse(
          statusCode = 404,
          data = ErrorResponse(
            errcode = "M_NOT_FOUND",
            error = "Account data not found.",
          ),
        )
      }

      else -> {
        request.sendResponse(
          statusCode = 405,
          data = ErrorResponse(
            errcode = "M_UNRECOGNIZED",
            error = "Method not allowed.",
          ),
        )
      }
    }
  }

  private fun getPushRules(request: MatrixRequest) {
    request.getAccessToken() ?: return

    // Stub: Return empty push rules
    request.sendRawJsonResponse(
      statusCode = 200,
      json =
        """
        {
          "global": {
            "override": [],
            "content": [],
            "room": [],
            "sender": [],
            "underride": []
          }
        }
        """.trimIndent(),
    )
  }

  private fun thirdPartyProtocols(request: MatrixRequest) {
    request.sendResponse(
      statusCode = 200,
      data = emptyMap<String, Any>(),
    )
  }

  private fun roomKeysVersion(request: MatrixRequest) {
    request.sendResponse(
      statusCode = 404,
      data = ErrorResponse(
        errcode = "M_NOT_FOUND",
        error = "No current backup version.",
      ),
    )
  }

  private fun MatrixRequest.getAccessToken(): UserSession? {
    if (accessToken == null) {
      sendResponse(
        statusCode = 400,
        data = ErrorResponse(
          errcode = "M_MISSING_TOKEN",
          error = "Missing access token.",
        ),
      )
      return null
    }

    val session = userSessions.find(accessToken!!)
    if (session == null) {
      sendResponse(
        statusCode = 401,
        data = ErrorResponse(
          errcode = "M_UNKNOWN_TOKEN",
          error = "Unrecognised access token.",
        ),
      )
      return null
    }

    return session
  }

  private fun MatrixRequest.sendResponse(
    statusCode: Int,
    data: Any,
  ) {
    json.serialize(data, responseStream)
    this.finishResponse(statusCode)
  }

  private fun MatrixRequest.sendRawJsonResponse(
    statusCode: Int,
    json: String,
  ) {
    responseStream.write(json.toByteArray())
    this.finishResponse(statusCode)
  }
}
