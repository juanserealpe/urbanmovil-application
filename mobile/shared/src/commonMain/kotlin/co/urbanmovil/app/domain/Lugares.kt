package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto

/** Radio dentro del cual un punto del recorrido toma el nombre de un lugar conocido. */
const val RADIO_NOMBRE_LUGAR_M = 150.0

/**
 * Lugar quemado más cercano a [punto] dentro de [maxM], para nombrar puntos del recorrido
 * («Pasa por Parque Caldas»). `null` si ninguno está lo bastante cerca.
 */
fun lugarMasCercano(punto: Punto, lugares: List<Lugar>, maxM: Double = RADIO_NOMBRE_LUGAR_M): Lugar? =
    lugares
        .map { it to distanciaM(punto, it.punto) }
        .filter { (_, d) -> d <= maxM }
        .minByOrNull { (_, d) -> d }
        ?.first
