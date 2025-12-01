package io.andrsn.matrix

import java.time.Instant

/**
 * Sessions for User-Interactive Authentication (UIA), i.e. the multi-step
 * authentication flows such as password, SSO, email confirmation, etc.
 *
 * TODO: automatic cleanup of unfinished sessions
 */
class AuthenticationSessions {

  private val sessions = HashMap<String, AuthenticationSession>()

  fun create(): AuthenticationSession {
    val sessionId = TokenGenerator.generate()
    val session = AuthenticationSession(
      sessionId = sessionId,
      createdAt = Instant.now(),
    )
    sessions[sessionId] = session
    return session
  }

  fun find(sessionId: String): AuthenticationSession? = sessions[sessionId]

  fun remove(sessionId: String) {
    sessions.remove(sessionId)
  }
}

data class AuthenticationSession(
  val sessionId: String,
  val createdAt: Instant,
)
