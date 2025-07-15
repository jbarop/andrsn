package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class VersionsResponse(

  @JsonAttribute(name = "unstable_features")
  val unstableFeatures: Map<String, Boolean>? = emptyMap(),

  val versions: List<String>,
)
