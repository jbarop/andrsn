package io.andrsn.matrix

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PasswordHasherTest {

  private val sut = PasswordHasher()

  @Test
  fun `should hash password`() {
    val password = "mySecurePassword123"
    val hash = sut.hashPassword(password)

    assertThat(hash).isNotBlank()
    assertThat(hash).isNotEqualTo(password)
    assertThat(hash).startsWith("\$argon2id\$")
  }

  @Test
  fun `should produce different hashes for same password`() {
    val password = "mySecurePassword123"
    val hash1 = sut.hashPassword(password)
    val hash2 = sut.hashPassword(password)

    assertThat(hash1).isNotEqualTo(hash2)
  }

  @Test
  fun `should verify correct password`() {
    val password = "mySecurePassword123"
    val hash = sut.hashPassword(password)

    val result = sut.verifyPassword(password, hash)

    assertThat(result).isTrue()
  }

  @Test
  fun `should reject incorrect password`() {
    val password = "mySecurePassword123"
    val hash = sut.hashPassword(password)

    val result = sut.verifyPassword("wrongPassword", hash)

    assertThat(result).isFalse()
  }

  @Test
  fun `should reject invalid hash format`() {
    val result = sut.verifyPassword("password", "invalid_hash")

    assertThat(result).isFalse()
  }

  @Test
  fun `should handle special characters`() {
    val password = "p@ssw0rd!#\$%^&*()"
    val hash = sut.hashPassword(password)

    assertThat(sut.verifyPassword(password, hash)).isTrue()
  }

  @Test
  fun `should handle unicode characters`() {
    val password = "пароль密码パスワード🔐"
    val hash = sut.hashPassword(password)

    assertThat(sut.verifyPassword(password, hash)).isTrue()
  }

  @Test
  fun `should reject null hash`() {
    val result = sut.verifyPassword("password", null)

    assertThat(result).isFalse()
  }

  @Test
  fun `should prevent timing attacks with null hash`() {
    val password = "testPassword123"

    // Measure time for null hash
    val startNull = System.nanoTime()
    val resultNull = sut.verifyPassword(password, null)
    val durationNull = System.nanoTime() - startNull

    // Measure time for invalid hash
    val invalidHash = sut.hashPassword("different")
    val startInvalid = System.nanoTime()
    val resultInvalid = sut.verifyPassword(password, invalidHash)
    val durationInvalid = System.nanoTime() - startInvalid

    // Both should return false
    assertThat(resultNull).isFalse()
    assertThat(resultInvalid).isFalse()

    // Execution times should be kept within the same order of magnitude to
    // reduce the risk of timing attacks.
    val ratio = durationNull.toDouble() / durationInvalid.toDouble()
    assertThat(ratio).isGreaterThan(0.1).isLessThan(10.0)
  }
}
