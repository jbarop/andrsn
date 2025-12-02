package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class LoginRequest(
  val type: String,
  val identifier: Identifier?,
  val password: String?,
  @field:JsonAttribute(name = "device_id")
  val deviceId: String?,
  @field:JsonAttribute(name = "initial_device_display_name")
  val initialDeviceDisplayName: String?,
) {
  @CompiledJson
  data class Identifier(
    val type: String,
    val user: String?,
  )
}
