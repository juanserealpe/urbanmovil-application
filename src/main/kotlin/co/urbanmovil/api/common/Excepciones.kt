package co.urbanmovil.api.common

/** Excepciones de negocio. El [GlobalExceptionHandler] las traduce al formato de §3.4. */
sealed class UrbanMovilException(mensaje: String) : RuntimeException(mensaje)

/** 400 VALIDACION: un dato no cumple una regla que Bean Validation no puede expresar. */
class ValidacionException(val campo: String, mensaje: String) : UrbanMovilException(mensaje)

/** 404 NO_ENCONTRADO. */
class NoEncontradoException(mensaje: String) : UrbanMovilException(mensaje)

/** 409 DATO_REPETIDO: uno o varios valores únicos ya existen. */
class DatoRepetidoException(val campos: List<CampoError>) : UrbanMovilException(
    if (campos.size == 1) campos.first().mensaje
    else "Hay datos que ya están registrados. Revise los campos señalados.",
)

/** 409 REGLA_NEGOCIO: una regla de negocio impide la acción. */
class ReglaNegocioException(mensaje: String) : UrbanMovilException(mensaje)

/** 401 CREDENCIALES_INVALIDAS. */
class CredencialesInvalidasException : UrbanMovilException("Credenciales inválidas")

/** 401 NO_AUTENTICADO (p. ej., la sesión apunta a un administrador que ya no existe). */
class NoAutenticadoException : UrbanMovilException(MENSAJE_NO_AUTENTICADO)

const val MENSAJE_NO_AUTENTICADO = "Debe iniciar sesión para continuar"
