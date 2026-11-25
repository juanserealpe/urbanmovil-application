package co.urbanmovil.api.programacion

import co.urbanmovil.api.recorrido.EnRecorridoResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

enum class EstadoRutaProgramada { PENDIENTE, EN_RECORRIDO, RETIRO_PENDIENTE }

data class ProgramacionRequest(
    @field:NotNull(message = "Indique las rutas de la programación")
    @field:Schema(description = "Lista completa y ordenada de ids de ruta", example = "[3, 1, 2]")
    val rutas: List<Long>?,
)

data class RutaProgramadaResponse(
    val rutaId: Long,
    val codigo: String,
    val nombre: String,
    val color: String,
    val orden: Int,
    val estado: EstadoRutaProgramada,
)

data class ProgramacionResponse(
    val conductorId: Long,
    val fecha: LocalDate,
    val rutas: List<RutaProgramadaResponse>,
)

data class ProgramacionResumenResponse(
    val conductorId: Long,
    val nombre: String,
    val disponible: Boolean,
    val enRecorrido: EnRecorridoResponse?,
    val totalRutas: Int,
)
