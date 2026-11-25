package co.urbanmovil.api.recorrido

import org.springframework.stereotype.Component

/** Resumen de un recorrido en curso, tal como lo muestran conductores y vehículos. */
data class EnRecorridoResponse(
    val recorridoId: Long,
    val rutaCodigo: String,
)

/**
 * Punto único para responder «¿tiene recorrido activo?» (D-05, D-08, D-11, RN-26).
 * Lo usan conductores, vehículos, rutas y programación para no duplicar la regla.
 * «Activo» = fila en `recorrido` con `estado = 'ACTIVO'`.
 */
@Component
class RecorridoActivoConsulta(private val recorridoRepository: RecorridoRepository) {

    fun conductorTieneActivo(conductorId: Long): Boolean =
        recorridoRepository.existsByConductorIdAndEstado(conductorId, EstadoRecorrido.ACTIVO)

    fun vehiculoTieneActivo(vehiculoId: Long): Boolean =
        recorridoRepository.existsByVehiculoIdAndEstado(vehiculoId, EstadoRecorrido.ACTIVO)

    fun rutaTieneActivos(rutaId: Long): Boolean =
        recorridoRepository.existsByRutaIdAndEstado(rutaId, EstadoRecorrido.ACTIVO)

    /** Id de la ruta que el conductor está recorriendo ahora, o `null`. */
    fun rutaEnCursoDelConductor(conductorId: Long): Long? =
        activos().firstOrNull { it.conductorId == conductorId }?.rutaId

    /** Cantidad de recorridos activos por id de ruta (las rutas sin recorridos no aparecen). */
    fun cantidadPorRuta(): Map<Long, Long> =
        recorridoRepository.contarPorRuta(EstadoRecorrido.ACTIVO).associate { it.rutaId to it.cantidad }

    /** Recorridos activos indexados por id de conductor (una sola consulta para toda la lista). */
    fun porConductor(): Map<Long, EnRecorridoResponse> =
        activos().associate { it.conductorId to EnRecorridoResponse(it.recorridoId, it.rutaCodigo) }

    /** Recorridos activos indexados por id de vehículo. */
    fun porVehiculo(): Map<Long, EnRecorridoResponse> =
        activos().associate { it.vehiculoId to EnRecorridoResponse(it.recorridoId, it.rutaCodigo) }

    private fun activos() = recorridoRepository.findFilasPorEstado(EstadoRecorrido.ACTIVO)
}
