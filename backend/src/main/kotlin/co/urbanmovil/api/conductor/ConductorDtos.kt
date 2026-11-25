package co.urbanmovil.api.conductor

import co.urbanmovil.api.recorrido.EnRecorridoResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

private const val REGEX_CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"

/** Contraseña mínima provisional (ligada a D-07). */
const val MINIMO_CONTRASENA = 8

data class ConductorCrearRequest(
    @field:NotBlank(message = "Escriba el nombre")
    @field:Size(max = 120, message = "El nombre admite máximo 120 caracteres")
    val nombre: String?,

    @field:NotBlank(message = "Escriba el número de identificación")
    @field:Size(max = 20, message = "La identificación admite máximo 20 caracteres")
    val identificacion: String?,

    @field:NotBlank(message = "Escriba el correo")
    @field:Email(regexp = REGEX_CORREO, message = "Escriba un correo válido")
    @field:Size(max = 120, message = "El correo admite máximo 120 caracteres")
    val correo: String?,

    @field:NotBlank(message = "Escriba el teléfono")
    @field:Size(max = 20, message = "El teléfono admite máximo 20 caracteres")
    val telefono: String?,

    @field:NotBlank(message = "Escriba la contraseña")
    @field:Size(min = MINIMO_CONTRASENA, message = "La contraseña debe tener al menos 8 caracteres")
    val contrasena: String?,
)

/** Modificar datos: la contraseña es opcional (si llega, se reemplaza). No cambia la disponibilidad. */
data class ConductorModificarRequest(
    @field:NotBlank(message = "Escriba el nombre")
    @field:Size(max = 120, message = "El nombre admite máximo 120 caracteres")
    val nombre: String?,

    @field:NotBlank(message = "Escriba el número de identificación")
    @field:Size(max = 20, message = "La identificación admite máximo 20 caracteres")
    val identificacion: String?,

    @field:NotBlank(message = "Escriba el correo")
    @field:Email(regexp = REGEX_CORREO, message = "Escriba un correo válido")
    @field:Size(max = 120, message = "El correo admite máximo 120 caracteres")
    val correo: String?,

    @field:NotBlank(message = "Escriba el teléfono")
    @field:Size(max = 20, message = "El teléfono admite máximo 20 caracteres")
    val telefono: String?,

    // Vacía o ausente = no se cambia; si llega, mínimo 8 caracteres.
    @field:Pattern(regexp = "^(|.{8,})$", message = "La contraseña debe tener al menos 8 caracteres")
    val contrasena: String? = null,
)

data class DisponibilidadRequest(
    @field:NotNull(message = "Indique si el conductor está disponible")
    val disponible: Boolean?,

    val motivo: MotivoNoDisponible? = null,

    @field:Size(max = 300, message = "El detalle admite máximo 300 caracteres")
    val detalle: String? = null,
)

data class ConductorResponse(
    val id: Long,
    val nombre: String,
    val identificacion: String,
    val correo: String,
    val telefono: String,
    val disponible: Boolean,
    val motivoNoDisponible: MotivoNoDisponible?,
    val detalleMotivo: String?,
    val enRecorrido: EnRecorridoResponse?,
    /** Códigos de las rutas de su programación de hoy, en orden. */
    val rutasHoy: List<String> = emptyList(),
) {
    companion object {
        fun de(conductor: Conductor, enRecorrido: EnRecorridoResponse?, rutasHoy: List<String> = emptyList()) = ConductorResponse(
            id = conductor.id!!,
            nombre = conductor.nombre,
            identificacion = conductor.identificacion,
            correo = conductor.correo,
            telefono = conductor.telefono,
            disponible = conductor.disponible,
            motivoNoDisponible = conductor.motivoNoDisponible,
            detalleMotivo = conductor.detalleMotivo,
            enRecorrido = enRecorrido,
            rutasHoy = rutasHoy,
        )
    }
}
