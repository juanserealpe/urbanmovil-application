package co.urbanmovil.api.demo

import co.urbanmovil.api.recorrido.EstadoRecorrido
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

data class IniciarRecorridoRequest(
    @field:NotNull(message = "Indique el conductor")
    @field:Schema(example = "1")
    val conductorId: Long?,

    @field:NotNull(message = "Indique la ruta")
    @field:Schema(example = "1")
    val rutaId: Long?,

    @field:NotNull(message = "Indique el vehículo")
    @field:Schema(example = "1")
    val vehiculoId: Long?,
)

data class ConductorRecorridoResponse(val id: Long, val nombre: String)

data class RutaRecorridoResponse(val id: Long, val codigo: String, val nombre: String)

data class VehiculoRecorridoResponse(val id: Long, val numeroInterno: String, val placa: String)

data class RecorridoDemoResponse(
    val id: Long,
    val conductor: ConductorRecorridoResponse,
    val ruta: RutaRecorridoResponse,
    val vehiculo: VehiculoRecorridoResponse,
    val estado: EstadoRecorrido,
    val inicio: OffsetDateTime,
    val fin: OffsetDateTime?,
)
