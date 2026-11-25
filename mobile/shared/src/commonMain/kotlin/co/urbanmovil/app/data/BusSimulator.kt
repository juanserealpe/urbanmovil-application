package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.domain.calcularVigencia
import co.urbanmovil.app.domain.longitudTotalM
import co.urbanmovil.app.domain.puntoEnPosicion

/**
 * Simulación SIN servidor: la posición de cada bus es una función pura de la hora (epoch millis).
 * Todos los teléfonos con la hora bien puesta ven lo mismo y reabrir la app no «reinicia» los buses.
 */
object BusSimulator {

    private const val MS_POR_MIN = 60_000L

    /** 0.0 = punto de inicio, 1.0 = punto final. Al terminar la vuelta vuelve a 0. */
    fun progresoEnVuelta(bus: Bus, ruta: Ruta, ahoraMs: Long): Double {
        val vueltaMs = vueltaMs(ruta)
        return msEnVuelta(bus, vueltaMs, ahoraMs).toDouble() / vueltaMs
    }

    fun estado(bus: Bus, ruta: Ruta, ahoraMs: Long): EstadoBus {
        val vueltaMs = vueltaMs(ruta)
        val t = msEnVuelta(bus, vueltaMs, ahoraMs)
        val minutoEnVuelta = (t / MS_POR_MIN).toInt()
        val ventana = bus.ventanaSinSenalMin

        // Dentro de la ventana sin señal el bus queda congelado donde estaba al inicio de la ventana.
        val (tEfectivo, horaUltimaUbicacion) = if (ventana != null && minutoEnVuelta in ventana) {
            val inicioVentana = ventana.first * MS_POR_MIN
            inicioVentana to ahoraMs - (t - inicioVentana)
        } else {
            t to ahoraMs
        }

        val posicionM = tEfectivo.toDouble() / vueltaMs * longitudTotalM(ruta.principal)
        return EstadoBus(
            bus = bus,
            posicion = puntoEnPosicion(ruta.principal, posicionM),
            posicionEnRecorridoM = posicionM,
            horaUltimaUbicacion = horaUltimaUbicacion,
            vigencia = calcularVigencia(ahoraMs - horaUltimaUbicacion),
        )
    }

    /** Estados de todos los buses cuya ruta exista, en el mismo orden de [buses]. */
    fun estados(buses: List<Bus>, rutas: List<Ruta>, ahoraMs: Long): List<EstadoBus> {
        val rutasPorId = rutas.associateBy { it.id }
        return buses.mapNotNull { bus -> rutasPorId[bus.rutaId]?.let { estado(bus, it, ahoraMs) } }
    }

    private fun vueltaMs(ruta: Ruta): Long {
        require(ruta.minutosPorVuelta > 0) { "minutosPorVuelta debe ser positivo (${ruta.id})" }
        return ruta.minutosPorVuelta * MS_POR_MIN
    }

    private fun msEnVuelta(bus: Bus, vueltaMs: Long, ahoraMs: Long): Long =
        (ahoraMs + bus.desfaseMin * MS_POR_MIN).mod(vueltaMs)
}
