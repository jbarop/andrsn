package io.andrsn.matrix

import de.mkammerer.argon2.Argon2Factory

class PasswordHasher {

  private val argon2 = Argon2Factory.create(
    /* type = */ Argon2Factory.Argon2Types.ARGON2id,
    /* defaultSaltLength = */ 32,
    /* defaultHashLength = */ 64,
  )
  private val memoryInKb = 65536
  private val parallelism = 1
  private val iterations = 3 // ~150ms on modern hardware

  fun hashPassword(password: String): String =
    argon2.hash(iterations, memoryInKb, parallelism, password.toCharArray())

  fun verifyPassword(
    password: String,
    hash: String,
  ): Boolean =
    try {
      argon2.verify(hash, password.toCharArray())
    } catch (_: Exception) {
      false
    }
}
