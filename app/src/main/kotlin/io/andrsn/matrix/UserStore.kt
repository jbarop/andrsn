package io.andrsn.matrix

class UserStore {

  private val users = HashMap<String, User>()

  fun addUser(user: User): User {
    users[user.username] = user
    return user
  }

  fun usernameExists(username: String): Boolean = users.containsKey(username)

  fun getUser(username: String): User? = users[username]
}

data class User(
  val username: String,
  val passwordHash: String,
)
