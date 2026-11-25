package co.urbanmovil.api.monitoreo

import co.urbanmovil.api.config.UrbanMovilProperties
import co.urbanmovil.api.recorrido.EstadoRecorrido
import co.urbanmovil.api.recorrido.Recorrido
import co.urbanmovil.api.recorrido.RecorridoRepository
import co.urbanmovil.api.ruta.Punto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime

@Service
class MonitoreoService(
    private val recorridoRepository: RecorridoRepository,
    private val propiedades: UrbanMovilProperties,
    private val clock: Clock,
) {

    /** HU-31 · CA-31.1 a CA-31.5. Un elemento por recorrido ACTIVO, con posición simulada. */
    @Transactional(readOnly = true)
    fun buses(): MonitoreoResponse {
        val foto = foto()
        val buses = foto.recorridos.map { (recorrido, posicion) ->
            val ruta = recorrido.ruta
            BusMonitoreo(
                recorridoId = recorrido.id!!,
                vehiculo = VehiculoMonitoreo(recorrido.vehiculo.id!!, recorrido.vehiculo.numeroInterno, recorrido.vehiculo.placa),
                ruta = RutaMonitoreo(ruta.id!!, ruta.codigo, ruta.nombre, ruta.color),
                conductor = ConductorMonitoreo(recorrido.conductor.id!!, recorrido.conductor.nombre),
                posicion = posicion,
                horaPosicion = foto.generadoEn,
                inicio = aHoraLocal(recorrido.inicio),
            )
        }
        return MonitoreoResponse(simulado = true, generadoEn = foto.generadoEn, velocidadKmh = foto.velocidadKmh, buses = buses)
    }

    /** Lo mismo que [buses] para la app del pasajero, sin conductor ni placa. */
    @Transactional(readOnly = true)
    fun busesPublicos(): BusesPublicosResponse {
        val foto = foto()
        val buses = foto.recorridos.map { (recorrido, posicion) ->
            BusPublico(
                recorridoId = recorrido.id!!,
                numeroInterno = recorrido.vehiculo.numeroInterno,
                rutaCodigo = recorrido.ruta.codigo,
                inicio = aHoraLocal(recorrido.inicio),
                posicion = posicion,
            )
        }
        return BusesPublicosResponse(simulado = true, generadoEn = foto.generadoEn, velocidadKmh = foto.velocidadKmh, buses = buses)
    }

    private class Foto(
        val generadoEn: OffsetDateTime,
        val velocidadKmh: Double,
        val recorridos: List<Pair<Recorrido, Punto>>,
    )

    /** Un único instante para todos los buses: así panel y app ven exactamente la misma foto. */
    private fun foto(): Foto {
        val ahora = clock.instant()
        val velocidad = propiedades.simulacion.velocidadKmh
        val recorridos = recorridoRepository.findCompletosPorEstado(EstadoRecorrido.ACTIVO)
            .filter { it.ruta.recorridoPrincipal.isNotEmpty() }
            .sortedBy { it.id }
            // Solo el recorrido principal; los alternos nunca se usan para cálculos (RN-03, CA-03.3)
            .map { it to SimuladorPosicion.posicion(it.ruta.recorridoPrincipal, it.inicio, ahora, velocidad) }
        return Foto(aHoraLocal(ahora), velocidad, recorridos)
    }

    private fun aHoraLocal(instante: Instant): OffsetDateTime = instante.atZone(clock.zone).toOffsetDateTime()
}
