package co.urbanmovil.api.monitoreo

import co.urbanmovil.api.ruta.Punto
import java.time.OffsetDateTime

data class VehiculoMonitoreo(val id: Long, val numeroInterno: String, val placa: String)

data class RutaMonitoreo(val id: Long, val codigo: String, val nombre: String, val color: String)

data class ConductorMonitoreo(val id: Long, val nombre: String)

data class BusMonitoreo(
    val recorridoId: Long,
    val vehiculo: VehiculoMonitoreo,
    val ruta: RutaMonitoreo,
    val conductor: ConductorMonitoreo,
    val posicion: Punto,
    val horaPosicion: OffsetDateTime,
)

data class MonitoreoResponse(
    /** Siempre `true` en este MVP: no hay GPS real (CA-31.5). */
    val simulado: Boolean,
    val generadoEn: OffsetDateTime,
    val buses: List<BusMonitoreo>,
)
