package io.andrsn.matrix.dto

import java.io.ByteArrayOutputStream

class MatrixRequest {
  lateinit var method: String
  lateinit var path: String
  var accessToken: String? = null
  lateinit var requestBody: ByteArray
  lateinit var responseStream: ByteArrayOutputStream
  lateinit var finishResponse: (statusCode: Int) -> Unit
}
