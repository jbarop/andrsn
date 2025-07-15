package io.andrsn.matrix

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class SupportedVersionsTest {

  @Test
  fun `it should support v1_11`() {
    assertThat(supportedVersions.versions).contains("v1.11")
  }

}
