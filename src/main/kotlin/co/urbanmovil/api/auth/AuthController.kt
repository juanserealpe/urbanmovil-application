package co.urbanmovil.api.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio y cierre de sesión del administrador (HU-01)")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    @SecurityRequirements // público
    @Operation(summary = "Iniciar sesión", description = "Devuelve el administrador y deja la cookie de sesión JSESSIONID")
    fun login(
        @Valid @RequestBody solicitud: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): AdministradorResponse = authService.iniciarSesion(solicitud.correo!!, solicitud.contrasena!!, request, response)

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        authService.cerrarSesion(request)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    @Operation(summary = "Administrador de la sesión actual")
    fun me(autenticacion: Authentication?): AdministradorResponse = authService.administradorActual(autenticacion)
}
