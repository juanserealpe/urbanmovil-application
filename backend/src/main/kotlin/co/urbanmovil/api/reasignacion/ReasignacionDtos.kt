package co.urbanmovil.api.reasignacion

import co.urbanmovil.api.programacion.ProgramacionResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class ReasignacionRequest(
    @field:NotBlank(message = "Escriba qué pasó")
    @field:Size(max = 300, message = "El motivo admite máximo 300 caracteres")
    @field:Schema(example = "Diana reportó falla mecánica")
    val motivo: String?,

    @field:Schema(description = "Ids de ruta que se agregan al final de la programación de hoy", example = "[2]")
    val rutasAsignar: List<Long>? = null,

    @field:Schema(description = "Ids de ruta que se quitan de la programación de hoy", example = "[1]")
    val rutasQuitar: List<Long>? = null,
)

data class PersonaResumen(
    val id: Long,
    val nombre: String,
)

data class ReasignacionResponse(
    val id: Long,
    val fechaHora: OffsetDateTime,
    val conductor: PersonaResumen,
    val administrador: PersonaResumen,
    val motivo: String,
    val rutasAsignadas: List<String>,
    val rutasQuitadas: List<String>,
    /** Programación de hoy tras aplicar la reasignación (solo en la respuesta del POST). */
    val programacion: ProgramacionResponse? = null,
)
