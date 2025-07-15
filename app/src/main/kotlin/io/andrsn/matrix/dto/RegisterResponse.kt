package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson

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
