package co.urbanmovil.api.monitoreo

import co.urbanmovil.api.ruta.Punto
import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Posición simulada de un bus según la hora (§6.2 de la Fase 3). Clase pura, sin Spring ni BD.
 *
 * Usa SOLO el recorrido principal (RN-03): el bus da vueltas a velocidad constante
 * desde `inicio`, así que cualquiera que consulte a la misma hora ve la misma posición.
 */
object SimuladorPosicion {

    private const val RADIO_TIERRA_METROS = 6_371_000.0

    /** Distancia haversine en metros entre dos puntos. */
    fun distanciaMetros(a: Punto, b: Punto): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * RADIO_TIERRA_METROS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Paso 1: suma de los tramos del recorrido principal, en metros. */
    fun longitudMetros(puntos: List<Punto>): Double =
        puntos.zipWithNext().sumOf { (a, b) -> distanciaMetros(a, b) }

    /** Pasos 2 a 5: fracción de la vuelta en curso, en [0, 1). */
    fun fraccion(longitudMetros: Double, inicio: Instant, ahora: Instant, velocidadKmh: Double): Double {
        if (longitudMetros <= 0.0 || velocidadKmh <= 0.0) return 0.0
        val velocidadMs = velocidadKmh * 1000.0 / 3600.0
        val duracionVueltaSeg = longitudMetros / velocidadMs
        val transcurridoSeg = Duration.between(inicio, ahora).toMillis().coerceAtLeast(0) / 1000.0
        return (transcurridoSeg % duracionVueltaSeg) / duracionVueltaSeg
    }

    /** Paso 6: punto a esa fracción de la longitud, interpolando linealmente dentro del tramo. */
    fun puntoEnFraccion(puntos: List<Punto>, fraccion: Double): Punto {
        require(puntos.isNotEmpty()) { "El recorrido principal no tiene puntos" }
        if (puntos.size == 1) return puntos.first()

        val tramos = puntos.zipWithNext()
        val longitudes = tramos.map { (a, b) -> distanciaMetros(a, b) }
        val total = longitudes.sum()
        if (total <= 0.0) return puntos.first()

        var restante = fraccion.coerceIn(0.0, 1.0) * total
        tramos.forEachIndexed { i, (a, b) ->
            val largo = longitudes[i]
            if (restante <= largo && largo > 0.0) {
                val t = restante / largo
                return Punto(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)
            }
            restante -= largo
        }
        return puntos.last()
    }

    /** Todo el cálculo: posición del bus a la hora `ahora`. */
    fun posicion(puntos: List<Punto>, inicio: Instant, ahora: Instant, velocidadKmh: Double): Punto =
        puntoEnFraccion(puntos, fraccion(longitudMetros(puntos), inicio, ahora, velocidadKmh))
}
