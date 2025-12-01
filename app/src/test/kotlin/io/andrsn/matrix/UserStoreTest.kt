package io.andrsn.matrix

import org.assertj.core.api.Assertions
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

    Assertions.assertThat(sut.usernameExists("testuser")).isFalse()

    sut.addUser(user)

    Assertions.assertThat(sut.usernameExists("testuser")).isTrue()
  }

  @Test
  fun `should not find non-existent user`() {
    Assertions.assertThat(sut.usernameExists("nonexistent")).isFalse()
  }

  @Test
  fun `should handle multiple users`() {
    sut.addUser(User("user1", "hash1"))
    sut.addUser(User("user2", "hash2"))
    sut.addUser(User("user3", "hash3"))

    Assertions.assertThat(sut.usernameExists("user1")).isTrue()
    Assertions.assertThat(sut.usernameExists("user2")).isTrue()
    Assertions.assertThat(sut.usernameExists("user3")).isTrue()
    Assertions.assertThat(sut.usernameExists("user4")).isFalse()
  }

  @Test
  fun `should overwrite user with same username`() {
    sut.addUser(User("testuser", "oldHash"))
    sut.addUser(User("testuser", "newHash"))

    Assertions.assertThat(sut.usernameExists("testuser")).isTrue()
  }

  @Test
  fun `should handle empty username`() {
    sut.addUser(User("", "hash"))

    Assertions.assertThat(sut.usernameExists("")).isTrue()
  }

  @Test
  fun `should handle special characters in username`() {
    sut.addUser(User("user_123.test-name", "hash"))

    Assertions.assertThat(sut.usernameExists("user_123.test-name")).isTrue()
  }
}
