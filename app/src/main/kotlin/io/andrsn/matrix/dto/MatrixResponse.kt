package io.andrsn.matrix.dto

data class MatrixResponse<T>(
  val statusCode: Int,
  val data: T,
)
