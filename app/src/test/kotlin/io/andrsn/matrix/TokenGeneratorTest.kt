package io.andrsn.matrix

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TokenGeneratorTest {

  private val sut = TokenGenerator

  @Test
  fun `it should generate token`() {
    val sessionId = sut.generate()
    assertThat(sessionId).hasSize(48)
  }

  @Test
  fun `should generate unique tokens`() {
    val ids = (1..1000).map { sut.generate() }.toSet()
    assertThat(ids).hasSize(1000)
  }
}
