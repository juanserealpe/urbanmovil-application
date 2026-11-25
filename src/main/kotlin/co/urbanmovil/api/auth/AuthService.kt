package co.urbanmovil.api.auth

import co.urbanmovil.api.administrador.AdministradorRepository
import co.urbanmovil.api.common.CredencialesInvalidasException
import co.urbanmovil.api.common.NoAutenticadoException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val securityContextRepository: SecurityContextRepository,
    private val administradorRepository: AdministradorRepository,
) {

    /**
     * Autentica y guarda el contexto en la sesión HTTP. Cualquier fallo (correo inexistente,
     * contraseña incorrecta, usuario que no es administrador) responde igual: «Credenciales inválidas».
     */
    @Transactional(readOnly = true)
    fun iniciarSesion(
        correo: String,
        contrasena: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): AdministradorResponse {
        val autenticacion = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(correo, contrasena),
            )
        } catch (ex: AuthenticationException) {
            throw CredencialesInvalidasException()
        }

        // Protección contra fijación de sesión: se descarta cualquier sesión previa.
        request.getSession(false)?.invalidate()
        request.getSession(true)

        val contexto = SecurityContextHolder.createEmptyContext().apply { authentication = autenticacion }
        SecurityContextHolder.setContext(contexto)
        securityContextRepository.saveContext(contexto, request, response)

        return administradorActual(autenticacion)
    }

    fun cerrarSesion(request: HttpServletRequest) {
        request.getSession(false)?.invalidate()
        SecurityContextHolder.clearContext()
    }

    @Transactional(readOnly = true)
    fun administradorActual(autenticacion: Authentication?): AdministradorResponse {
        val correo = autenticacion?.name ?: throw NoAutenticadoException()
        val administrador = administradorRepository.findByCorreoIgnoreCase(correo) ?: throw NoAutenticadoException()
        return AdministradorResponse.de(administrador)
    }
}
