package io.andrsn.matrix

import io.andrsn.matrix.dto.LoginTypesResponse
import io.andrsn.matrix.dto.MatrixResponse
import io.andrsn.matrix.dto.RegisterRequest
import io.andrsn.matrix.dto.RegisterResponse
import io.andrsn.matrix.dto.VersionsResponse

class Matrix {

  fun getSupportedVersions(): MatrixResponse<VersionsResponse> =
    MatrixResponse(
      statusCode = 200,
      data =
        VersionsResponse(
          unstableFeatures = emptyMap(),
          versions =
            listOf(
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
        ),
    )

  fun getLoginTypes(): MatrixResponse<LoginTypesResponse> =
    MatrixResponse(
      statusCode = 200,
      data =
        LoginTypesResponse(
          flows =
            setOf(
              LoginTypesResponse.Flow(type = "m.login.password"),
            ),
        ),
    )

  fun register(request: RegisterRequest): MatrixResponse<RegisterResponse> =
    MatrixResponse(
      statusCode = 401,
      data =
        RegisterResponse(
          session = SessionGenerator.generateSessionId(),
          flows =
            setOf(
              RegisterResponse.Flow(
                listOf("m.login.email.identity"),
              ),
            ),
        ),
    )
}
