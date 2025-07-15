package io.andrsn.matrix

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SessionGeneratorTest {

  private val sut = SessionGenerator

  @Test
  fun `it should generate session id`() {
    val sessionId = sut.generateSessionId()
    assertThat(sessionId).hasSize(48)
  }
}
