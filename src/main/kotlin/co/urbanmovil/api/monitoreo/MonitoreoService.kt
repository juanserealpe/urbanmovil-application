package co.urbanmovil.api.monitoreo

import co.urbanmovil.api.config.UrbanMovilProperties
import co.urbanmovil.api.recorrido.EstadoRecorrido
import co.urbanmovil.api.recorrido.RecorridoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class MonitoreoService(
    private val recorridoRepository: RecorridoRepository,
    private val propiedades: UrbanMovilProperties,
    private val clock: Clock,
) {

    /** HU-31 · CA-31.1 a CA-31.5. Un elemento por recorrido ACTIVO, con posición simulada. */
    @Transactional(readOnly = true)
    fun buses(): MonitoreoResponse {
        val ahora = clock.instant()
        val horaPosicion = ahora.atZone(clock.zone).toOffsetDateTime()
        val velocidad = propiedades.simulacion.velocidadKmh

        val buses = recorridoRepository.findCompletosPorEstado(EstadoRecorrido.ACTIVO)
            .filter { it.ruta.recorridoPrincipal.isNotEmpty() }
            .sortedBy { it.id }
            .map { recorrido ->
                val ruta = recorrido.ruta
                BusMonitoreo(
                    recorridoId = recorrido.id!!,
                    vehiculo = VehiculoMonitoreo(recorrido.vehiculo.id!!, recorrido.vehiculo.numeroInterno, recorrido.vehiculo.placa),
                    ruta = RutaMonitoreo(ruta.id!!, ruta.codigo, ruta.nombre, ruta.color),
                    conductor = ConductorMonitoreo(recorrido.conductor.id!!, recorrido.conductor.nombre),
                    // Solo el recorrido principal; los alternos nunca se usan para cálculos (RN-03, CA-03.3)
                    posicion = SimuladorPosicion.posicion(ruta.recorridoPrincipal, recorrido.inicio, ahora, velocidad),
                    horaPosicion = horaPosicion,
                )
            }
        return MonitoreoResponse(simulado = true, generadoEn = horaPosicion, buses = buses)
    }
}
