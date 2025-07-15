package io.andrsn

import io.andrsn.http.startHttpServer
import io.andrsn.matrix.Matrix

fun main() {
  val matrix = Matrix()
  startHttpServer(matrix)
  println("I know kung fu.")
}
