package io.andrsn.matrix

import java.security.SecureRandom
import java.util.Base64

object TokenGenerator {

  private val secureRandom = SecureRandom()
  private val base64Encoder = Base64.getUrlEncoder().withoutPadding()
  private val buffer = ByteArray(36) // 36 bytes --> 48 base64 string

  fun generate(): String {
    secureRandom.nextBytes(buffer)
    return base64Encoder.encodeToString(buffer)
  }
}
