package io.andrsn.matrix

class UserStore {

  private val users = mutableMapOf<String, User>()

  fun addUser(user: User) {
    users[user.username] = user
  }

  fun usernameExists(username: String): Boolean = users.containsKey(username)
}

data class User(
  val username: String,
  val passwordHash: String,
)
