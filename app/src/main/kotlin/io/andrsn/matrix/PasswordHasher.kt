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
  private val iterations = 4 // ~150ms on modern hardware
  private val dummyHash = hashPassword("dummy_password")

  fun hashPassword(password: String): String =
    argon2.hash(iterations, memoryInKb, parallelism, password.toCharArray())

  fun verifyPassword(
    password: String,
    hash: String?,
  ): Boolean =
    try {
      if (hash !== null) {
        argon2.verify(hash, password.toCharArray())
      } else {
        // fake hashing for timing attack prevention
        argon2.verify(dummyHash, password.toCharArray())
        false
      }
    } catch (_: Exception) {
      false
    }
}
