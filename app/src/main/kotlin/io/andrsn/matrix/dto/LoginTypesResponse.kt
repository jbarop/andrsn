package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson

@CompiledJson
data class LoginTypesResponse(
  val flows: Set<Flow>,
) {

  @CompiledJson
  data class Flow(
    val type: String,
  )
}
