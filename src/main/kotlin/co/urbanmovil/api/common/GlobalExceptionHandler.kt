package co.urbanmovil.api.common

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import tools.jackson.core.JacksonException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // ---------- Excepciones propias ----------

    @ExceptionHandler(ValidacionException::class)
    fun validacion(ex: ValidacionException) =
        responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, ex.message!!, listOf(CampoError(ex.campo, ex.message!!)))

    @ExceptionHandler(NoEncontradoException::class)
    fun noEncontrado(ex: NoEncontradoException) =
        responder(HttpStatus.NOT_FOUND, CodigoError.NO_ENCONTRADO, ex.message!!)

    @ExceptionHandler(DatoRepetidoException::class)
    fun datoRepetido(ex: DatoRepetidoException) =
        responder(HttpStatus.CONFLICT, CodigoError.DATO_REPETIDO, ex.message!!, ex.campos)

    @ExceptionHandler(ReglaNegocioException::class)
    fun reglaNegocio(ex: ReglaNegocioException) =
        responder(HttpStatus.CONFLICT, CodigoError.REGLA_NEGOCIO, ex.message!!)

    @ExceptionHandler(CredencialesInvalidasException::class)
    fun credencialesInvalidas(ex: CredencialesInvalidasException) =
        responder(HttpStatus.UNAUTHORIZED, CodigoError.CREDENCIALES_INVALIDAS, ex.message!!)

    @ExceptionHandler(NoAutenticadoException::class)
    fun noAutenticado(ex: NoAutenticadoException) =
        responder(HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO, ex.message!!)

    // ---------- Errores de entrada ----------

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun beanValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val campos = ex.bindingResult.fieldErrors
            .map { CampoError(it.field, it.defaultMessage ?: "Valor inválido") }
            .distinctBy { it.campo } // un mensaje por campo, el primero que falló
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, mensajeValidacion(campos), campos)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun constraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val campos = ex.constraintViolations.map {
            CampoError(it.propertyPath.toString().substringAfterLast('.'), it.message)
        }
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, mensajeValidacion(campos), campos)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun cuerpoIlegible(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val campo = (ex.cause as? JacksonException)?.path
            ?.mapNotNull { it.propertyName }
            ?.lastOrNull()
        return if (campo != null) {
            val mensaje = "El valor enviado no es válido"
            responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, mensaje, listOf(CampoError(campo, mensaje)))
        } else {
            responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, "El cuerpo de la solicitud no es válido")
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun tipoInvalido(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        val mensaje = "El valor enviado no es válido"
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, mensaje, listOf(CampoError(ex.name, mensaje)))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun parametroFaltante(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        val mensaje = "Este dato es obligatorio"
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, mensaje, listOf(CampoError(ex.parameterName, mensaje)))
    }

    // ---------- Otros ----------

    /** Respaldo ante carreras: el índice único de la BD detectó un duplicado que el servicio no vio. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun integridad(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.warn("Violación de integridad no anticipada por el servicio", ex)
        return responder(HttpStatus.CONFLICT, CodigoError.DATO_REPETIDO, "Ya existe un registro con estos datos")
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun rutaInexistente(ex: NoResourceFoundException) =
        responder(HttpStatus.NOT_FOUND, CodigoError.NO_ENCONTRADO, "El recurso solicitado no existe")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodoNoPermitido(ex: HttpRequestMethodNotSupportedException) =
        responder(HttpStatus.METHOD_NOT_ALLOWED, CodigoError.METODO_NO_PERMITIDO, "Método HTTP no permitido para este recurso")

    @ExceptionHandler(Exception::class)
    fun inesperado(ex: Exception): ResponseEntity<ErrorResponse> {
        // Excepciones de Spring MVC que ya traen su código HTTP (415, 406, ...): se respetan.
        if (ex is org.springframework.web.ErrorResponse && ex.statusCode.is4xxClientError()) {
            return ResponseEntity.status(ex.statusCode)
                .body(ErrorResponse(CodigoError.VALIDACION, "La solicitud no es válida"))
        }
        log.error("Error no controlado", ex)
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO, "Ocurrió un error inesperado. Intente de nuevo.")
    }

    private fun mensajeValidacion(campos: List<CampoError>): String =
        if (campos.size == 1) campos.first().mensaje else "Hay campos con errores. Revise los datos ingresados."

    private fun responder(
        estado: HttpStatus,
        codigo: String,
        mensaje: String,
        campos: List<CampoError> = emptyList(),
    ): ResponseEntity<ErrorResponse> = ResponseEntity.status(estado).body(ErrorResponse(codigo, mensaje, campos))
}
