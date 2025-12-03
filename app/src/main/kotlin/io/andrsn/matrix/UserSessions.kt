package io.andrsn.matrix

import java.time.Instant

/**
 * Sessions of authenticated users.
 *
 * TODO: cleanup of session, max session lifetime
 */
class UserSessions {

  private val sessions = mutableMapOf<String, UserSession>()

  fun create(
    userId: String,
    deviceId: String,
  ): UserSession {
    val newAccessToken = TokenGenerator.generate()
    val newSession = UserSession(
      accessToken = newAccessToken,
      userId = userId,
      deviceId = deviceId,
      createdAt = Instant.now(),
    )
    sessions[newAccessToken] = newSession
    return newSession
  }

  fun find(accessToken: String): UserSession? = sessions[accessToken]

  fun delete(accessToken: String) {
    sessions.remove(accessToken)
  }

  fun deleteAllForUser(userId: String) {
    sessions.values.removeIf { it.userId == userId }
  }
}

data class UserSession(
  val accessToken: String,
  val userId: String,
  val deviceId: String,
  val createdAt: Instant,
)
