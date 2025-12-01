package io.andrsn.matrix

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthenticationSessionsTest {

  private lateinit var sut: AuthenticationSessions

  @BeforeEach
  fun setup() {
    sut = AuthenticationSessions()
  }

  @Test
  fun `should create session`() {
    val session = sut.create()

    assertThat(session.sessionId).isNotBlank()
    assertThat(session.sessionId).hasSize(48)
    assertThat(session.createdAt).isNotNull()
  }

  @Test
  fun `should find created session`() {
    val session = sut.create()

    val found = sut.find(session.sessionId)

    assertThat(found).isEqualTo(session)
  }

  @Test
  fun `should not find non-existent session`() {
    val found = sut.find("nonexistent")

    assertThat(found).isNull()
  }

  @Test
  fun `should remove session`() {
    val session = sut.create()

    sut.remove(session.sessionId)

    assertThat(sut.find(session.sessionId)).isNull()
  }

  @Test
  fun `should handle removing non-existent session`() {
    sut.remove("nonexistent")
  }

  @Test
  fun `should create multiple unique sessions`() {
    val session1 = sut.create()
    val session2 = sut.create()
    val session3 = sut.create()

    assertThat(session1.sessionId).isNotEqualTo(session2.sessionId)
    assertThat(session2.sessionId).isNotEqualTo(session3.sessionId)
    assertThat(session1.sessionId).isNotEqualTo(session3.sessionId)
  }

  @Test
  fun `should create session with current timestamp`() {
    val before = Instant.now()
    val session = sut.create()
    val after = Instant.now()

    assertThat(session.createdAt).isBetween(before, after)
  }
}
