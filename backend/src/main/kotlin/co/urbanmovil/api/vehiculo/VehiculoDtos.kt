package co.urbanmovil.api.vehiculo

import co.urbanmovil.api.recorrido.EnRecorridoResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

/** Se usa para registrar y para modificar (PUT): ambos piden placa y número interno. */
data class VehiculoRequest(
    @field:NotBlank(message = "Escriba la placa")
    @field:Size(max = 10, message = "La placa admite máximo 10 caracteres")
    val placa: String?,

    @field:NotBlank(message = "Escriba el número interno")
    @field:Size(max = 20, message = "El número interno admite máximo 20 caracteres")
    val numeroInterno: String?,
)

data class EstadoVehiculoRequest(
    @field:NotNull(message = "Indique si el vehículo queda activo")
    val activo: Boolean?,

    /** D-15 (provisional): texto libre, obligatorio al desactivar. */
    @field:Size(max = 300, message = "El motivo admite máximo 300 caracteres")
    val motivo: String? = null,
)

data class VehiculoResponse(
    val id: Long,
    val placa: String,
    val numeroInterno: String,
    val activo: Boolean,
    val motivoInactivacion: String?,
    val fechaInactivacion: OffsetDateTime?,
    val enRecorrido: EnRecorridoResponse?,
)
