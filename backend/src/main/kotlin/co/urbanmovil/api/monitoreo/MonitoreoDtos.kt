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
    /** Con [inicio] y la velocidad, el cliente puede calcular la posición entre consultas (animación). */
    val inicio: OffsetDateTime,
)

data class MonitoreoResponse(
    /** Siempre `true` en este MVP: no hay GPS real (CA-31.5). */
    val simulado: Boolean,
    val generadoEn: OffsetDateTime,
    /** Velocidad constante de la simulación; los clientes la usan con `inicio` para animar. */
    val velocidadKmh: Double,
    val buses: List<BusMonitoreo>,
)

/** Bus para la app del pasajero: sin conductor ni placa (datos personales / operativos). */
data class BusPublico(
    val recorridoId: Long,
    val numeroInterno: String,
    /** Coincide con `codigo` de `GET /api/v1/public/rutas`. */
    val rutaCodigo: String,
    val inicio: OffsetDateTime,
    val posicion: Punto,
)

data class BusesPublicosResponse(
    val simulado: Boolean,
    /** Hora del servidor: el cliente la usa para corregir el desfase de su propio reloj. */
    val generadoEn: OffsetDateTime,
    val velocidadKmh: Double,
    val buses: List<BusPublico>,
)
