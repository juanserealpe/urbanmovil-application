package co.urbanmovil.app.ui.rutas

import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.data.model.Vigencia
import co.urbanmovil.app.domain.InfoOcupacion
import co.urbanmovil.app.domain.busesActivosPorRuta
import co.urbanmovil.app.domain.busesVisibles
import co.urbanmovil.app.domain.formatearEdad
import co.urbanmovil.app.domain.formatearHora12
import co.urbanmovil.app.domain.infoOcupacion

/** Un bus tal como se muestra en el mapa, la lista y la tarjeta. Nunca representa un bus OCULTO. */
data class BusUi(
    val id: String,
    val rutaId: String,
    val rutaNombre: String,
    /** ARGB, igual que [Ruta.colorHex]. */
    val colorRuta: Long,
    val punto: Punto,
    val desactualizado: Boolean,
    /** «7:42:18 p. m.» en hora de Colombia. */
    val horaUltimaUbicacion: String,
    /** «hace 12 s». */
    val edadUbicacion: String,
    val ocupacion: InfoOcupacion,
)

/** Fila de la lista de rutas (HU-18). */
data class ResumenRuta(val ruta: Ruta, val busesActivos: Int)

data class RutasUi(
    val rutas: List<ResumenRuta>,
    val seleccionadaId: String?,
    val buses: List<BusUi>,
    val busSeleccionado: BusUi?,
    val miUbicacion: Lugar,
)

data class DetalleRutaUi(
    /** `null` si el id no corresponde a ninguna ruta. */
    val ruta: Ruta?,
    val buses: List<BusUi>,
    val busSeleccionado: BusUi?,
)

fun EstadoBus.aBusUi(ruta: Ruta?, ahoraMs: Long): BusUi = BusUi(
    id = bus.id,
    rutaId = bus.rutaId,
    rutaNombre = ruta?.nombre.orEmpty(),
    colorRuta = ruta?.colorHex ?: 0xFF4B5563,
    punto = posicion,
    desactualizado = vigencia == Vigencia.DESACTUALIZADA,
    horaUltimaUbicacion = formatearHora12(horaUltimaUbicacion),
    edadUbicacion = formatearEdad(ahoraMs - horaUltimaUbicacion),
    ocupacion = infoOcupacion(bus),
)

/** Pestaña Rutas: todas las rutas con su conteo y todos los buses no OCULTOS. */
fun construirRutasUi(
    rutas: List<Ruta>,
    estados: List<EstadoBus>,
    ahoraMs: Long,
    seleccionadaId: String?,
    busSeleccionadoId: String?,
    miUbicacion: Lugar,
): RutasUi {
    val rutasPorId = rutas.associateBy { it.id }
    val conteo = busesActivosPorRuta(estados)
    val buses = busesVisibles(estados).map { it.aBusUi(rutasPorId[it.bus.rutaId], ahoraMs) }
    return RutasUi(
        rutas = rutas.map { ResumenRuta(it, conteo[it.id] ?: 0) },
        seleccionadaId = seleccionadaId?.takeIf { it in rutasPorId },
        buses = buses,
        busSeleccionado = buses.firstOrNull { it.id == busSeleccionadoId },
        miUbicacion = miUbicacion,
    )
}

/** Detalle de una ruta: solo sus buses no OCULTOS (CA-19.3, CA-19.4). */
fun construirDetalleRutaUi(
    ruta: Ruta?,
    estados: List<EstadoBus>,
    ahoraMs: Long,
    busSeleccionadoId: String?,
): DetalleRutaUi {
    val buses = if (ruta == null) emptyList() else busesVisibles(estados, ruta.id).map { it.aBusUi(ruta, ahoraMs) }
    return DetalleRutaUi(
        ruta = ruta,
        buses = buses,
        busSeleccionado = buses.firstOrNull { it.id == busSeleccionadoId },
    )
}
