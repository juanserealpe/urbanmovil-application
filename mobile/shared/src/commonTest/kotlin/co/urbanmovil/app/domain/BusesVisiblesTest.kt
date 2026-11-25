package co.urbanmovil.app.domain

import co.urbanmovil.app.data.FakeData
import co.urbanmovil.app.data.FakeTransitRepository
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Vigencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BusesVisiblesTest {

    private val estados = listOf(
        Fixtures.estado("R1", 0.0, Vigencia.RECIENTE, busId = "A"),
        Fixtures.estado("R1", 10.0, Vigencia.DESACTUALIZADA, busId = "B"),
        Fixtures.estado("R1", 20.0, Vigencia.OCULTA, busId = "C"),
        Fixtures.estado("R2", 0.0, Vigencia.RECIENTE, busId = "D"),
        Fixtures.estado("R3", 0.0, Vigencia.OCULTA, busId = "E"),
    )

    @Test
    fun busesVisibles_descartaOcultos() =
        assertEquals(listOf("A", "B", "D"), busesVisibles(estados).map { it.bus.id })

    @Test
    fun busesVisibles_filtraPorRuta() =
        assertEquals(listOf("A", "B"), busesVisibles(estados, "R1").map { it.bus.id })

    @Test
    fun busesVisibles_rutaSoloConOcultos_quedaVacia() = assertTrue(busesVisibles(estados, "R3").isEmpty())

    @Test
    fun busesActivosPorRuta_cuentaSoloNoOcultos() {
        val conteo = busesActivosPorRuta(estados)
        assertEquals(2, conteo["R1"])
        assertEquals(1, conteo["R2"])
        assertNull(conteo["R3"])
    }

    @Test
    fun demo_busDeR3_desapareceEnSuVentanaLarga() {
        // UM-412 tiene desfase 0 y vuelta de 40 min: en el minuto 22 lleva 17 min sin señal → OCULTA.
        val vueltaR3Ms = FakeData.rutas.first { it.id == "R3" }.minutosPorVuelta * 60_000L
        val repo = FakeTransitRepository(reloj = { vueltaR3Ms * 1_000 + 22 * 60_000L })
        val estadosR3 = busesVisibles(repo.estadosEn(repo.ahoraMs()), "R3")
        assertTrue(estadosR3.isEmpty())
    }

    @Test
    fun limites_encierranTodosLosPuntos() {
        val l = limites(listOf(Punto(2.0, -76.0), Punto(2.5, -76.7), Punto(2.2, -76.3)))!!
        assertEquals(Limites(sur = 2.0, oeste = -76.7, norte = 2.5, este = -76.0), l)
    }

    @Test
    fun limites_listaVacia_esNull() = assertNull(limites(emptyList()))
}
