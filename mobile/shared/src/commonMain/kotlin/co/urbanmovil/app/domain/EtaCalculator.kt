package co.urbanmovil.app.domain

import co.urbanmovil.app.data.FakeData
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.data.model.Vigencia
import kotlin.math.ceil

/** HU-20 / HU-22. Resultado de «¿en cuánto pasa?». */
sealed interface ResultadoEta {
    /** El punto del recorrido más cercano está a más de [FakeData.MAX_CAMINATA_M] (CA-20.4). */
    data object LejosDeLaRuta : ResultadoEta

    /** No hay bus RECIENTE aguas arriba del punto (RN-11, CA-20.3). */
    data object SinBusProximo : ResultadoEta

    data class Llega(
        val minutos: Int,
        val puntoDelRecorrido: ProyeccionEnRecorrido,
        val busId: String,
    ) : ResultadoEta
}

/** ETA del próximo bus de [ruta] al punto del recorrido principal más cercano a [punto]. */
fun calcularEta(
    punto: Punto,
    ruta: Ruta,
    estados: List<EstadoBus>,
    velocidadKmh: Double = FakeData.VELOCIDAD_PROMEDIO_KMH,
    maxCaminataM: Double = FakeData.MAX_CAMINATA_M,
): ResultadoEta {
    // RN-03: solo el recorrido principal.
    val objetivo = puntoMasCercano(punto, ruta.principal)
    if (objetivo.distanciaM > maxCaminataM) return ResultadoEta.LejosDeLaRuta
    return etaHastaPosicion(objetivo, ruta.id, estados, velocidadKmh)
}

/** ETA del bus RECIENTE más cercano aguas arriba de [objetivo]. Los DESACTUALIZADOS y OCULTOS no cuentan. */
fun etaHastaPosicion(
    objetivo: ProyeccionEnRecorrido,
    rutaId: String,
    estados: List<EstadoBus>,
    velocidadKmh: Double = FakeData.VELOCIDAD_PROMEDIO_KMH,
): ResultadoEta {
    val bus = estados
        .filter { it.bus.rutaId == rutaId && it.vigencia == Vigencia.RECIENTE }
        .filter { it.posicionEnRecorridoM <= objetivo.posicionM } // aún no ha pasado
        .maxByOrNull { it.posicionEnRecorridoM }
        ?: return ResultadoEta.SinBusProximo

    val metros = distanciaSobreRecorrido(bus.posicionEnRecorridoM, objetivo.posicionM)
    return ResultadoEta.Llega(minutosPorDistancia(metros, velocidadKmh), objetivo, bus.bus.id)
}

/** Minutos de viaje en bus entre dos posiciones del recorrido. */
fun tiempoTrayecto(
    abordajeM: Double,
    descensoM: Double,
    velocidadKmh: Double = FakeData.VELOCIDAD_PROMEDIO_KMH,
): Int = minutosPorDistancia(distanciaSobreRecorrido(abordajeM, descensoM), velocidadKmh)

/** Redondea hacia arriba, mínimo 1 minuto. */
internal fun minutosPorDistancia(metros: Double, velocidadKmh: Double): Int {
    require(velocidadKmh > 0) { "La velocidad debe ser positiva" }
    val metrosPorMinuto = velocidadKmh * 1_000.0 / 60.0
    return ceil(metros / metrosPorMinuto).toInt().coerceAtLeast(1)
}
