package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.domain.longitudTotalM
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contrato compartido con `SimuladorPosicionTest` (backend) y `simulador.test.ts` (web):
 * mismo trazado, misma hora, mismos números. Si este test cambia, hay que cambiar los tres.
 */
class SimuladorRecorridoTest {

    private val trazado = listOf(
        Punto(2.4419, -76.6066),
        Punto(2.4440, -76.6050),
        Punto(2.4462, -76.6031),
        Punto(2.4490, -76.6010),
        Punto(2.4510, -76.5990),
    )
    private val inicio = 1_795_532_400_000L // 2026-11-24T10:00:00-05:00

    private fun cerca(esperado: Punto, real: Punto) =
        assertTrue(abs(esperado.lat - real.lat) < 1e-9 && abs(esperado.lng - real.lng) < 1e-9, "Se esperaba $esperado pero fue $real")

    @Test
    fun longitudDelTrazado() {
        assertTrue(abs(longitudTotalM(trazado) - 1319.9881103444893) < 1e-6)
    }

    @Test
    fun recienIniciado_estaEnElPrimerPunto() {
        assertEquals(trazado.first(), SimuladorRecorrido.posicion(trazado, inicio, inicio, 18.0).punto)
    }

    @Test
    fun a90s_coincideConElBackend() {
        cerca(Punto(2.445065833931734, -76.60407950705896), SimuladorRecorrido.posicion(trazado, inicio, inicio + 90_000, 18.0).punto)
    }

    @Test
    fun a600s_yaDioLaVuelta() {
        cerca(Punto(2.444453192996366, -76.6046086060486), SimuladorRecorrido.posicion(trazado, inicio, inicio + 600_000, 18.0).punto)
    }

    @Test
    fun posicionEnMetros_esLaFraccionDeLaLongitud() {
        // 90 s a 5 m/s = 450 m
        assertTrue(abs(SimuladorRecorrido.posicion(trazado, inicio, inicio + 90_000, 18.0).metros - 450.0) < 1e-6)
    }

    @Test
    fun antesDelInicio_seQuedaEnElPrimerPunto() {
        assertEquals(trazado.first(), SimuladorRecorrido.posicion(trazado, inicio, inicio - 30_000, 18.0).punto)
    }
}
