package io.andrsn.matrix

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserStoreTest {

  private lateinit var sut: UserStore

  @BeforeEach
  fun setup() {
    sut = UserStore()
  }

  @Test
  fun `should add and check user existence`() {
    val user = User("testuser", "hash123")

    assertThat(sut.usernameExists("testuser")).isFalse()

    sut.addUser(user)

    assertThat(sut.usernameExists("testuser")).isTrue()
  }

  @Test
  fun `should not find non-existent user`() {
    assertThat(sut.usernameExists("nonexistent")).isFalse()
  }

  @Test
  fun `should handle multiple users`() {
    sut.addUser(User("user1", "hash1"))
    sut.addUser(User("user2", "hash2"))
    sut.addUser(User("user3", "hash3"))

    assertThat(sut.usernameExists("user1")).isTrue()
    assertThat(sut.usernameExists("user2")).isTrue()
    assertThat(sut.usernameExists("user3")).isTrue()
    assertThat(sut.usernameExists("user4")).isFalse()
  }

  @Test
  fun `should retrieve existing user`() {
    val user = User("testuser", "hash123")
    sut.addUser(user)

    val retrievedUser = sut.getUser("testuser")

    assertThat(retrievedUser).isNotNull
    assertThat(retrievedUser?.username).isEqualTo("testuser")
    assertThat(retrievedUser?.passwordHash).isEqualTo("hash123")
  }

  @Test
  fun `should return null for non-existent user`() {
    val retrievedUser = sut.getUser("nonexistent")

    assertThat(retrievedUser).isNull()
  }

  @Test
  fun `should retrieve correct user from multiple users`() {
    sut.addUser(User("user1", "hash1"))
    sut.addUser(User("user2", "hash2"))
    sut.addUser(User("user3", "hash3"))

    val user1 = sut.getUser("user1")
    val user2 = sut.getUser("user2")
    val user3 = sut.getUser("user3")

    assertThat(user1?.username).isEqualTo("user1")
    assertThat(user1?.passwordHash).isEqualTo("hash1")
    assertThat(user2?.username).isEqualTo("user2")
    assertThat(user2?.passwordHash).isEqualTo("hash2")
    assertThat(user3?.username).isEqualTo("user3")
    assertThat(user3?.passwordHash).isEqualTo("hash3")
  }
}
