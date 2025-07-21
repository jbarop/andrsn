package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class RegisterRequest(

  @field:JsonAttribute(name = "auth")
  val authentication: Authentication?,

  @field:JsonAttribute(name = "initial_device_display_name")
  val initialDeviceDisplayName: String?,
) {

  @CompiledJson
  data class Authentication(
    val session: String,
  )
}
