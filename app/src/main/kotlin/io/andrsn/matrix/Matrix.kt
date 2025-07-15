package io.andrsn.matrix

import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.RegisterRequest
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.VersionsResponse

class Matrix {

  fun getSupportedVersions(): VersionsResponse =
    VersionsResponse(
      unstableFeatures = emptyMap(),
      versions = listOf(
        "v1.1",
        "v1.2",
        "v1.3",
        "v1.4",
        "v1.5",
        "v1.6",
        "v1.7",
        "v1.8",
        "v1.9",
        "v1.10",
        "v1.11",
      ),
    )

  fun getLoginTypes(): LoginTypesResponse =
    LoginTypesResponse(
      flows = setOf(
        LoginTypesResponse.Flow(type = "m.login.password"),
      ),
    )

  fun register(request: RegisterRequest): RegisterResponse {
    return RegisterResponse(
      session = SessionGenerator.generateSessionId(),
      flows = setOf(
        RegisterResponse.Flow(
          listOf("m.login.email.identity")
        ),
      ),
    )
  }

}
