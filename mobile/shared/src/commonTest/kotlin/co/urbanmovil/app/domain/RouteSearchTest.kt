package co.urbanmovil.app.domain

import co.urbanmovil.app.data.FakeData
import co.urbanmovil.app.data.model.Punto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RouteSearchTest {

    private val oeste = Punto(Fixtures.LAT, -76.62)
    private val este = Punto(Fixtures.LAT, -76.60)

    private fun buscar(origen: Punto, destino: Punto, vararg rutas: co.urbanmovil.app.data.model.Ruta) =
        buscarViajes(origen, destino, rutas.toList(), estados = emptyList())

    @Test
    fun rutaEnSentidoContrario_noAparece() {
        val ida = Fixtures.ruta("IDA", Fixtures.recta)
        val vuelta = Fixtures.ruta("VUELTA", Fixtures.recta.reversed())

        val opciones = assertIs<ResultadoBusqueda.Opciones>(buscar(oeste, este, ida, vuelta)).opciones

        assertEquals(listOf("IDA"), opciones.map { it.ruta.id })
    }

    @Test
    fun ordenaPorMenorCaminataAlAbordaje() {
        val lejana = Fixtures.ruta("LEJANA", Fixtures.recta.map { it.copy(lat = it.lat + 0.002) }) // ≈ 222 m
        val cercana = Fixtures.ruta("CERCANA", Fixtures.recta.map { it.copy(lat = it.lat + 0.0005) }) // ≈ 55 m

        val opciones = assertIs<ResultadoBusqueda.Opciones>(buscar(oeste, este, lejana, cercana)).opciones

        assertEquals(listOf("CERCANA", "LEJANA"), opciones.map { it.ruta.id })
    }

    @Test
    fun empateEnAbordaje_desempataPorMenorCaminataDesdeElDescenso() {
        // Ambas arrancan en el origen (caminata 0); "TORCIDA" termina ≈ 220 m al norte del destino.
        val torcida = Fixtures.ruta("TORCIDA", listOf(oeste, Punto(Fixtures.LAT + 0.002, -76.60)))
        val directa = Fixtures.ruta("DIRECTA", listOf(oeste, este))

        val opciones = assertIs<ResultadoBusqueda.Opciones>(buscar(oeste, este, torcida, directa)).opciones

        assertEquals(listOf("DIRECTA", "TORCIDA"), opciones.map { it.ruta.id })
        assertEquals(opciones[0].caminataAbordajeM, opciones[1].caminataAbordajeM, 1e-6)
        assertTrue(opciones[0].caminataDescensoM < opciones[1].caminataDescensoM)
    }

    @Test
    fun caminataMayorA500m_seDescarta() {
        val muyLejana = Fixtures.ruta("MUY_LEJANA", Fixtures.recta.map { it.copy(lat = it.lat + 0.006) }) // ≈ 667 m
        val opciones = assertIs<ResultadoBusqueda.Opciones>(buscar(oeste, este, muyLejana)).opciones
        assertTrue(opciones.isEmpty())
    }

    @Test
    fun puntosIguales_esError() {
        assertEquals(ResultadoBusqueda.PuntosIguales, buscar(oeste, oeste, Fixtures.rutaRecta))
    }

    @Test
    fun puntosAMenosDe20m_esError() {
        val casiIgual = Punto(Fixtures.LAT + 0.0001, -76.62) // ≈ 11 m
        assertEquals(ResultadoBusqueda.PuntosIguales, buscar(oeste, casiIgual, Fixtures.rutaRecta))
    }

    @Test
    fun losAlternosNuncaSeUsan() {
        // El principal está ≈ 2.2 km al norte; solo el alterno pasa por origen y destino.
        val ruta = Fixtures.ruta(
            id = "CON_ALTERNO",
            principal = Fixtures.recta.map { it.copy(lat = it.lat + 0.02) },
            alternos = listOf(Fixtures.recta),
        )
        val opciones = assertIs<ResultadoBusqueda.Opciones>(buscar(oeste, este, ruta)).opciones
        assertTrue(opciones.isEmpty())
    }

    @Test
    fun opcionIncluyeTramosEtaYTrayecto() {
        val estados = listOf(Fixtures.estado("RT", 0.0))
        val origen = Punto(Fixtures.LAT, -76.618)
        val resultado = buscarViajes(origen, este, listOf(Fixtures.rutaRecta), estados, velocidadKmh = 18.0)

        val opcion = assertIs<ResultadoBusqueda.Opciones>(resultado).opciones.single()
        assertEquals("RT", opcion.id)
        assertTrue(opcion.descenso.posicionM > opcion.abordaje.posicionM)
        assertIs<ResultadoEta.Llega>(opcion.etaAbordaje)
        assertTrue(opcion.minutosTrayecto > 0)
    }

    @Test
    fun demo_ValenciaATulcan_encuentraDosOpciones() {
        val valencia = FakeData.lugares.first { it.nombre == "Barrio Valencia" }.punto
        val tulcan = FakeData.lugares.first { it.nombre.contains("Tulcán") }.punto

        val opciones = assertIs<ResultadoBusqueda.Opciones>(
            buscarViajes(valencia, tulcan, FakeData.rutas, estados = emptyList()),
        ).opciones

        assertEquals(2, opciones.size, "Opciones: ${opciones.map { it.ruta.id }}")
    }
}
