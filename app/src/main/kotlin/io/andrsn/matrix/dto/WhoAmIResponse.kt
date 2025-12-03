package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class WhoAmIResponse(
  @field:JsonAttribute(name = "user_id")
  val userId: String,
  @field:JsonAttribute(name = "device_id")
  val deviceId: String,
)
