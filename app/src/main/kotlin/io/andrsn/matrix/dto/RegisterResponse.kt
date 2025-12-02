package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class UsernameAvailableResponse(
  val available: Boolean,
)

@CompiledJson
data class RegisterResponse(
  val session: String,
  val flows: Set<Flow>,
) {

  @CompiledJson
  data class Flow(
    val stages: List<String>,
  )
}

@CompiledJson
data class RegisterSuccessResponse(
  @field:JsonAttribute(name = "user_id")
  val userId: String,
  @field:JsonAttribute(name = "home_server")
  val homeServer: String,
  @field:JsonAttribute(name = "access_token")
  val accessToken: String?,
  @field:JsonAttribute(name = "device_id")
  val deviceId: String?,
)
