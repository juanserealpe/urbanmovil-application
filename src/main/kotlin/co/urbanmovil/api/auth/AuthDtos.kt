package co.urbanmovil.api.auth

import co.urbanmovil.api.administrador.Administrador
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Escriba el correo")
    val correo: String?,

    @field:NotBlank(message = "Escriba la contraseña")
    val contrasena: String?,
)

data class AdministradorResponse(
    val id: Long,
    val nombre: String,
    val correo: String,
) {
    companion object {
        fun de(administrador: Administrador) =
            AdministradorResponse(administrador.id!!, administrador.nombre, administrador.correo)
    }
}
