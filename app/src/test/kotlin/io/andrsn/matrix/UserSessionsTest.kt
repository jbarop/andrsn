package io.andrsn.matrix

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class UserSessionsTest {

  private lateinit var sut: UserSessions

  @BeforeEach
  fun setup() {
    sut = UserSessions()
  }

  @Test
  fun `should create session with access token`() {
    val session = sut.create(
      userId = "@user:localhost",
      deviceId = "device123",
    )

    assertThat(session.accessToken).hasSize(48)
    assertThat(session.userId).isEqualTo("@user:localhost")
    assertThat(session.deviceId).isEqualTo("device123")
    assertThat(session.createdAt).isBeforeOrEqualTo(Instant.now())
  }

  @Test
  fun `should find session by access token`() {
    val createdSession = sut.create(
      userId = "@user:localhost",
      deviceId = "device123",
    )

    val foundSession = sut.find(createdSession.accessToken)

    assertThat(foundSession).isNotNull
    assertThat(foundSession?.accessToken).isEqualTo(createdSession.accessToken)
    assertThat(foundSession?.userId).isEqualTo("@user:localhost")
    assertThat(foundSession?.deviceId).isEqualTo("device123")
  }

  @Test
  fun `should return null for non-existent access token`() {
    val foundSession = sut.find("non_existent_token")

    assertThat(foundSession).isNull()
  }

  @Test
  fun `should generate unique access tokens`() {
    val session1 = sut.create(
      userId = "@user1:localhost",
      deviceId = "device1",
    )
    val session2 = sut.create(
      userId = "@user2:localhost",
      deviceId = "device2",
    )

    assertThat(session1.accessToken).isNotEqualTo(session2.accessToken)
  }

  @Test
  fun `should handle multiple sessions`() {
    val session1 = sut.create(
      userId = "@user1:localhost",
      deviceId = "device1",
    )
    val session2 = sut.create(
      userId = "@user2:localhost",
      deviceId = "device2",
    )
    val session3 = sut.create(
      userId = "@user3:localhost",
      deviceId = "device3",
    )

    assertThat(sut.find(session1.accessToken)?.userId)
      .isEqualTo("@user1:localhost")
    assertThat(sut.find(session2.accessToken)?.userId)
      .isEqualTo("@user2:localhost")
    assertThat(sut.find(session3.accessToken)?.userId)
      .isEqualTo("@user3:localhost")
  }

  @Test
  fun `should allow same user with different devices`() {
    val session1 = sut.create(
      userId = "@user:localhost",
      deviceId = "device1",
    )
    val session2 = sut.create(
      userId = "@user:localhost",
      deviceId = "device2",
    )

    assertThat(session1.accessToken).isNotEqualTo(session2.accessToken)
    assertThat(sut.find(session1.accessToken)?.deviceId).isEqualTo("device1")
    assertThat(sut.find(session2.accessToken)?.deviceId).isEqualTo("device2")
  }

  @Test
  fun `should set creation timestamp`() {
    val beforeCreation = Instant.now()
    val session = sut.create(
      userId = "@user:localhost",
      deviceId = "device123",
    )
    val afterCreation = Instant.now()

    assertThat(session.createdAt).isBetween(beforeCreation, afterCreation)
  }

  @Test
  fun `should delete session by access token`() {
    val session = sut.create(
      userId = "@user:localhost",
      deviceId = "device123",
    )

    assertThat(sut.find(session.accessToken)).isNotNull()

    sut.delete(session.accessToken)

    assertThat(sut.find(session.accessToken)).isNull()
  }

  @Test
  fun `should not affect other sessions when deleting one`() {
    val session1 = sut.create(
      userId = "@user1:localhost",
      deviceId = "device1",
    )
    val session2 = sut.create(
      userId = "@user2:localhost",
      deviceId = "device2",
    )

    sut.delete(session1.accessToken)

    assertThat(sut.find(session1.accessToken)).isNull()
    assertThat(sut.find(session2.accessToken)).isNotNull()
  }

  @Test
  fun `should handle deleting non-existent session`() {
    // Should not throw exception
    sut.delete("non_existent_token")
  }

  @Test
  fun `should delete all sessions for user`() {
    val session1 = sut.create(
      userId = "@user:localhost",
      deviceId = "device1",
    )
    val session2 = sut.create(
      userId = "@user:localhost",
      deviceId = "device2",
    )
    val session3 = sut.create(
      userId = "@user:localhost",
      deviceId = "device3",
    )

    sut.deleteAllForUser("@user:localhost")

    assertThat(sut.find(session1.accessToken)).isNull()
    assertThat(sut.find(session2.accessToken)).isNull()
    assertThat(sut.find(session3.accessToken)).isNull()
  }

  @Test
  fun `should only delete sessions for specified user`() {
    val user1Session1 = sut.create(
      userId = "@user1:localhost",
      deviceId = "device1",
    )
    val user2Session = sut.create(
      userId = "@user2:localhost",
      deviceId = "device3",
    )

    sut.deleteAllForUser("@user1:localhost")

    assertThat(sut.find(user1Session1.accessToken)).isNull()
    assertThat(sut.find(user2Session.accessToken)).isNotNull
  }
}
