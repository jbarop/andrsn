package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson

@CompiledJson
data class ErrorResponse(
  val errcode: String,
  val error: String,
)
