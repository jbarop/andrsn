package io.andrsn.matrix.dto

import java.io.ByteArrayOutputStream

class MatrixRequest {
  var method: Method? = null
  lateinit var requestBody: ByteArray
  lateinit var responseStream: ByteArrayOutputStream
  lateinit var finishResponse: (statusCode: Int) -> Unit

  enum class Method(
    val path: String,
  ) {
    CLIENT_GET_VERSIONS("/_matrix/client/versions"),
    CLIENT_GET_LOGIN("/_matrix/client/v3/login"),
    CLIENT_POST_REGISTER("/_matrix/client/v3/register"),
  }
}
