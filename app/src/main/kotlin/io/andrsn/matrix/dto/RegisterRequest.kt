package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class RegisterRequest(

  @JsonAttribute(name = "auth")
  val authentication: Authentication?,

  @JsonAttribute(name = "initial_device_display_name")
  val initialDeviceDisplayName: String?,
) {

  @CompiledJson
  data class Authentication(
    val session: String
  )

}
