package co.urbanmovil.api.monitoreo

import co.urbanmovil.api.ruta.Punto
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pruebas unitarias puras (sin Spring) del cálculo de §6.2. */
class SimuladorPosicionTest {

    /** Sobre un mismo meridiano la distancia es proporcional a la latitud: tramos de 1 y 3 unidades. */
    private val recta = listOf(Punto(0.0, 0.0), Punto(0.001, 0.0), Punto(0.004, 0.0))

    private val inicio = Instant.parse("2026-11-24T15:00:00Z")

    private fun cerca(esperado: Punto, real: Punto, tolerancia: Double = 1e-9) {
        assertTrue(
            abs(esperado.lat - real.lat) < tolerancia && abs(esperado.lng - real.lng) < tolerancia,
            "Se esperaba $esperado pero fue $real",
        )
    }

    @Test
    fun `fraccion 0 es el primer punto`() {
        cerca(Punto(0.0, 0.0), SimuladorPosicion.puntoEnFraccion(recta, 0.0))
    }

    @Test
    fun `fraccion 0_5 es el punto medio por distancia y no por cantidad de puntos`() {
        // Por cantidad de puntos el medio sería el 2º (0.001); por distancia es 0.002
        cerca(Punto(0.002, 0.0), SimuladorPosicion.puntoEnFraccion(recta, 0.5))
    }

    @Test
    fun `fraccion 1 es el ultimo punto`() {
        cerca(Punto(0.004, 0.0), SimuladorPosicion.puntoEnFraccion(recta, 1.0))
    }

    @Test
    fun `interpola linealmente dentro del tramo`() {
        cerca(Punto(0.0005, 0.0), SimuladorPosicion.puntoEnFraccion(recta, 0.125))
    }

    @Test
    fun `en el instante de inicio esta en el primer punto`() {
        cerca(Punto(0.0, 0.0), SimuladorPosicion.posicion(recta, inicio, inicio, 18.0))
    }

    @Test
    fun `una vuelta completa vuelve al inicio`() {
        val longitud = SimuladorPosicion.longitudMetros(recta)
        val duracionVuelta = longitud / (18.0 * 1000 / 3600) // segundos
        // Redondeo hacia arriba: un milisegundo de menos dejaría al bus al FINAL de la vuelta, no al inicio
        val ahora = inicio.plus(Duration.ofMillis(ceil(duracionVuelta * 1000).toLong()))

        cerca(Punto(0.0, 0.0), SimuladorPosicion.posicion(recta, inicio, ahora, 18.0), tolerancia = 1e-6)
    }

    @Test
    fun `a media vuelta esta en el punto medio`() {
        val longitud = SimuladorPosicion.longitudMetros(recta)
        val mediaVuelta = longitud / (18.0 * 1000 / 3600) / 2
        val ahora = inicio.plus(Duration.ofMillis((mediaVuelta * 1000).roundToLong()))

        cerca(Punto(0.002, 0.0), SimuladorPosicion.posicion(recta, inicio, ahora, 18.0), tolerancia = 1e-6)
    }

    @Test
    fun `la posicion avanza con el tiempo`() {
        val antes = SimuladorPosicion.posicion(recta, inicio, inicio.plusSeconds(10), 18.0)
        val despues = SimuladorPosicion.posicion(recta, inicio, inicio.plusSeconds(20), 18.0)
        assertTrue(despues.lat > antes.lat)
    }

    @Test
    fun `una hora anterior al inicio se queda en el primer punto`() {
        cerca(Punto(0.0, 0.0), SimuladorPosicion.posicion(recta, inicio, inicio.minusSeconds(30), 18.0))
    }

    @Test
    fun `haversine de un grado de latitud es unos 111 km`() {
        val metros = SimuladorPosicion.distanciaMetros(Punto(0.0, 0.0), Punto(1.0, 0.0))
        assertEquals(111_195.0, metros, 1.0)
    }

    @Test
    fun `trazado de un solo punto o de longitud cero devuelve ese punto`() {
        val unico = listOf(Punto(2.44, -76.6))
        cerca(Punto(2.44, -76.6), SimuladorPosicion.posicion(unico, inicio, inicio.plusSeconds(60), 18.0))
        val quieto = listOf(Punto(2.44, -76.6), Punto(2.44, -76.6))
        cerca(Punto(2.44, -76.6), SimuladorPosicion.posicion(quieto, inicio, inicio.plusSeconds(60), 18.0))
    }
}
