package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Vigencia
import co.urbanmovil.app.domain.Fixtures
import co.urbanmovil.app.domain.longitudTotalM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusSimulatorTest {

    private val min = 60_000L
    private val ruta = Fixtures.ruta("RT", Fixtures.recta, minutosPorVuelta = 20)
    private val largo = longitudTotalM(ruta.principal)

    /** Múltiplo exacto de la vuelta → minuto 0 de la vuelta para un bus sin desfase. */
    private val inicioVuelta = 1_000L * 20 * min

    private val bus = Bus(id = "B1", rutaId = "RT", conductor = "c", desfaseMin = 0, ventanaSinSenalMin = 5..8)

    @Test
    fun mismaHora_mismaPosicion() {
        val ahora = inicioVuelta + 3 * min + 12_345
        val a = BusSimulator.estado(bus, ruta, ahora)
        val b = BusSimulator.estado(bus, ruta, ahora)
        assertEquals(a, b)
    }

    @Test
    fun fueraDeLaVentana_ubicacionEsAhoraYReciente() {
        val ahora = inicioVuelta + 2 * min
        val estado = BusSimulator.estado(bus, ruta, ahora)
        assertEquals(ahora, estado.horaUltimaUbicacion)
        assertEquals(Vigencia.RECIENTE, estado.vigencia)
        assertEquals(largo * 2 / 20, estado.posicionEnRecorridoM, 1e-6)
    }

    @Test
    fun dentroDeLaVentana_noSeMueveYSuEdadCrece() {
        val t1 = inicioVuelta + 6 * min
        val t2 = inicioVuelta + 7 * min + 30_000
        val e1 = BusSimulator.estado(bus, ruta, t1)
        val e2 = BusSimulator.estado(bus, ruta, t2)

        assertEquals(e1.posicion, e2.posicion)
        assertEquals(e1.posicionEnRecorridoM, e2.posicionEnRecorridoM, 1e-9)
        // Congelado en el inicio de la ventana (minuto 5).
        assertEquals(largo * 5 / 20, e1.posicionEnRecorridoM, 1e-6)
        assertEquals(inicioVuelta + 5 * min, e1.horaUltimaUbicacion)
        assertEquals(e1.horaUltimaUbicacion, e2.horaUltimaUbicacion)
        assertTrue(t2 - e2.horaUltimaUbicacion > t1 - e1.horaUltimaUbicacion)
        assertEquals(Vigencia.DESACTUALIZADA, e2.vigencia)
    }

    @Test
    fun ventanaLarga_ocultaElBus() {
        val busLargo = bus.copy(ventanaSinSenalMin = 1..18)
        val estado = BusSimulator.estado(busLargo, ruta, inicioVuelta + 17 * min + 1)
        assertEquals(Vigencia.OCULTA, estado.vigencia)
    }

    @Test
    fun alTerminarLaVuelta_vuelveAlInicio() {
        val estado = BusSimulator.estado(bus, ruta, inicioVuelta + 20 * min)
        assertEquals(0.0, estado.posicionEnRecorridoM, 1e-9)
        assertEquals(ruta.principal.first(), estado.posicion)
    }

    @Test
    fun desfase_separaLosBuses() {
        val adelantado = bus.copy(id = "B2", desfaseMin = 10, ventanaSinSenalMin = null)
        val estado = BusSimulator.estado(adelantado, ruta, inicioVuelta)
        assertEquals(largo / 2, estado.posicionEnRecorridoM, 1e-6)
    }

    @Test
    fun avanzaProporcionalALaDistancia() {
        // Primer tramo ≈ 111 m y segundo ≈ 2.1 km: a mitad de vuelta el bus está en el segundo tramo.
        val desigual = Fixtures.ruta(
            "DES",
            listOf(Punto(2.44, -76.620), Punto(2.44, -76.619), Punto(2.44, -76.600)),
            minutosPorVuelta = 20,
        )
        val sinVentana = bus.copy(rutaId = "DES", ventanaSinSenalMin = null)
        val estado = BusSimulator.estado(sinVentana, desigual, inicioVuelta + 10 * min)
        assertEquals(-76.610, estado.posicion.lng, 1e-4)
    }

    @Test
    fun progresoEnVuelta_vaDeCeroAUno() {
        assertEquals(0.0, BusSimulator.progresoEnVuelta(bus, ruta, inicioVuelta), 1e-12)
        assertEquals(0.25, BusSimulator.progresoEnVuelta(bus, ruta, inicioVuelta + 5 * min), 1e-12)
    }
}
