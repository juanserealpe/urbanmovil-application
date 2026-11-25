package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.domain.longitudTotalM
import co.urbanmovil.app.domain.puntoEnPosicion

/**
 * Port de `SimuladorPosicion.kt` del backend: el bus da vueltas al recorrido principal a velocidad
 * constante desde `inicio`. Es la MISMA fórmula que usan el servidor y la web, así que los tres ven
 * el bus en el mismo lugar sin pedirle la posición al servidor en cada cuadro.
 *
 * (El [BusSimulator] de la Fase 1 sigue para el modo sin backend: usa otra regla, por minutos por vuelta.)
 */
object SimuladorRecorrido {

    /** Dónde está el bus: el punto en el mapa y los metros recorridos desde el inicio del trazado. */
    data class Posicion(val punto: Punto, val metros: Double)

    fun posicion(principal: List<Punto>, inicioMs: Long, ahoraMs: Long, velocidadKmh: Double): Posicion {
        require(principal.isNotEmpty()) { "El recorrido principal no tiene puntos" }
        val longitud = longitudTotalM(principal)
        val metros = fraccion(longitud, inicioMs, ahoraMs, velocidadKmh) * longitud
        return Posicion(puntoEnPosicion(principal, metros), metros)
    }

    /** Fracción de la vuelta en curso, en [0, 1). Antes de `inicio` es 0. */
    private fun fraccion(longitudM: Double, inicioMs: Long, ahoraMs: Long, velocidadKmh: Double): Double {
        if (longitudM <= 0.0 || velocidadKmh <= 0.0) return 0.0
        val duracionVueltaSeg = longitudM / (velocidadKmh * 1000.0 / 3600.0)
        val transcurridoSeg = (ahoraMs - inicioMs).coerceAtLeast(0) / 1000.0
        return (transcurridoSeg % duracionVueltaSeg) / duracionVueltaSeg
    }

    /** Duración de una vuelta en minutos enteros (lo que pide [co.urbanmovil.app.data.model.Ruta]). */
    fun minutosPorVuelta(principal: List<Punto>, velocidadKmh: Double): Int {
        val longitud = longitudTotalM(principal)
        if (longitud <= 0.0 || velocidadKmh <= 0.0) return 1
        val minutos = longitud / (velocidadKmh * 1000.0 / 60.0)
        return kotlin.math.ceil(minutos).toInt().coerceAtLeast(1)
    }
}
