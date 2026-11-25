package co.urbanmovil.api.common

/** Cuerpo único de error para toda la API (§3.4). */
data class ErrorResponse(
    val codigo: String,
    val mensaje: String,
    val campos: List<CampoError> = emptyList(),
)

data class CampoError(
    val campo: String,
    val mensaje: String,
)

object CodigoError {
    const val VALIDACION = "VALIDACION"
    const val NO_AUTENTICADO = "NO_AUTENTICADO"
    const val CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS"
    const val ACCESO_DENEGADO = "ACCESO_DENEGADO"
    const val NO_ENCONTRADO = "NO_ENCONTRADO"
    const val DATO_REPETIDO = "DATO_REPETIDO"
    const val REGLA_NEGOCIO = "REGLA_NEGOCIO"
    const val METODO_NO_PERMITIDO = "METODO_NO_PERMITIDO"
    const val ERROR_INTERNO = "ERROR_INTERNO"
}
