package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class RegisterRequest(
  val username: String?,
  val password: String?,
  @field:JsonAttribute(name = "device_id")
  val deviceId: String?,
  @field:JsonAttribute(name = "inhibit_login")
  val inhibitLogin: Boolean?,
  @field:JsonAttribute(name = "auth")
  val authentication: Authentication?,
  @field:JsonAttribute(name = "initial_device_display_name")
  val initialDeviceDisplayName: String?,
) {
  @CompiledJson
  data class Authentication(
    val type: String?,
    val session: String?,
  )
}
