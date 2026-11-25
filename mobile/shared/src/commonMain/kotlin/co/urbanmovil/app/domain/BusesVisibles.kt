package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Vigencia

/**
 * Buses que se pueden mostrar (CA-19.3: los OCULTOS no existen para el pasajero).
 * @param rutaId si se indica, solo los de esa ruta.
 */
fun busesVisibles(estados: List<EstadoBus>, rutaId: String? = null): List<EstadoBus> =
    estados.filter { it.vigencia != Vigencia.OCULTA && (rutaId == null || it.bus.rutaId == rutaId) }

/**
 * Buses RECIENTES de una ruta: los únicos que cuentan para ETA (RN-11). Si hay alguno y aun así el
 * resultado es «Sin bus próximo», es que ya pasaron; si no hay ninguno, falta ubicación reciente.
 */
fun contarBusesRecientes(estados: List<EstadoBus>, rutaId: String): Int =
    estados.count { it.bus.rutaId == rutaId && it.vigencia == Vigencia.RECIENTE }

/** Número de buses activos (no OCULTOS) por ruta; las rutas sin buses no aparecen en el mapa. */
fun busesActivosPorRuta(estados: List<EstadoBus>): Map<String, Int> =
    busesVisibles(estados).groupingBy { it.bus.rutaId }.eachCount()
