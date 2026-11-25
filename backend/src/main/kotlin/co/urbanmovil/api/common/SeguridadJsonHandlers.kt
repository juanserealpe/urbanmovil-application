package co.urbanmovil.api.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/** Sin sesión: 401 JSON (nunca redirige ni muestra el formulario de login de Spring). */
@Component
class JsonAuthenticationEntryPoint(private val jsonMapper: JsonMapper) : AuthenticationEntryPoint {
    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        escribir(response, HttpStatus.UNAUTHORIZED, ErrorResponse(CodigoError.NO_AUTENTICADO, MENSAJE_NO_AUTENTICADO), jsonMapper)
    }
}

/** Con sesión pero sin el rol requerido: 403 JSON. */
@Component
class JsonAccessDeniedHandler(private val jsonMapper: JsonMapper) : AccessDeniedHandler {
    override fun handle(request: HttpServletRequest, response: HttpServletResponse, accessDeniedException: AccessDeniedException) {
        escribir(
            response,
            HttpStatus.FORBIDDEN,
            ErrorResponse(CodigoError.ACCESO_DENEGADO, "No tiene permiso para realizar esta acción"),
            jsonMapper,
        )
    }
}

private fun escribir(response: HttpServletResponse, estado: HttpStatus, cuerpo: ErrorResponse, jsonMapper: JsonMapper) {
    response.status = estado.value()
    response.contentType = MediaType.APPLICATION_JSON_VALUE
    response.characterEncoding = Charsets.UTF_8.name()
    response.writer.write(jsonMapper.writeValueAsString(cuerpo))
}
