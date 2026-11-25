package co.urbanmovil.app.domain

import co.urbanmovil.app.data.FakeData
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta

/** Una forma de ir de origen a destino con UNA sola ruta (RN-10: sin transbordos). */
data class OpcionViaje(
    /** Identificador estable para navegar a `guia/{opcionId}`. Una opción por ruta → se usa el id de la ruta. */
    val id: String,
    val ruta: Ruta,
    val abordaje: ProyeccionEnRecorrido,
    val descenso: ProyeccionEnRecorrido,
    val etaAbordaje: ResultadoEta,
    val minutosTrayecto: Int,
) {
    val caminataAbordajeM: Double get() = abordaje.distanciaM
    val caminataDescensoM: Double get() = descenso.distanciaM
}

sealed interface ResultadoBusqueda {
    /** Origen y destino coinciden o están a menos de [FakeData.DISTANCIA_PUNTOS_IGUALES_M] (CA-21.5). */
    data object PuntosIguales : ResultadoBusqueda

    /** Lista ordenada; vacía si ninguna ruta sirve. */
    data class Opciones(val opciones: List<OpcionViaje>) : ResultadoBusqueda
}

/**
 * HU-21. Por cada ruta, abordaje y descenso = puntos del recorrido principal más cercanos a origen y destino.
 * Se descarta la ruta si alguna caminata supera [maxCaminataM] (RN-08) o si el descenso no queda después del
 * abordaje en el sentido de circulación (CA-21.3). Los alternos nunca se usan (RN-03).
 *
 * Orden: menor caminata al abordaje y, en empate, menor caminata desde el descenso (CA-21.2).
 * Los tiempos NO alteran el orden (DP-15).
 */
fun buscarViajes(
    origen: Punto,
    destino: Punto,
    rutas: List<Ruta>,
    estados: List<EstadoBus>,
    maxCaminataM: Double = FakeData.MAX_CAMINATA_M,
    velocidadKmh: Double = FakeData.VELOCIDAD_PROMEDIO_KMH,
    distanciaPuntosIgualesM: Double = FakeData.DISTANCIA_PUNTOS_IGUALES_M,
): ResultadoBusqueda {
    if (distanciaM(origen, destino) < distanciaPuntosIgualesM) return ResultadoBusqueda.PuntosIguales

    val opciones = rutas.mapNotNull { ruta ->
        val abordaje = puntoMasCercano(origen, ruta.principal)
        val descenso = puntoMasCercano(destino, ruta.principal)
        when {
            abordaje.distanciaM > maxCaminataM -> null
            descenso.distanciaM > maxCaminataM -> null
            descenso.posicionM <= abordaje.posicionM -> null
            else -> OpcionViaje(
                id = ruta.id,
                ruta = ruta,
                abordaje = abordaje,
                descenso = descenso,
                etaAbordaje = etaHastaPosicion(abordaje, ruta.id, estados, velocidadKmh),
                minutosTrayecto = tiempoTrayecto(abordaje.posicionM, descenso.posicionM, velocidadKmh),
            )
        }
    }.sortedWith(compareBy<OpcionViaje>({ it.caminataAbordajeM }, { it.caminataDescensoM }))

    return ResultadoBusqueda.Opciones(opciones)
}
