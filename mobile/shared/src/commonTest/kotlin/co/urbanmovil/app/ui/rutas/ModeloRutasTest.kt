package co.urbanmovil.app.ui.rutas

import co.urbanmovil.app.data.FakeData
import co.urbanmovil.app.data.FakeTransitRepository
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Vigencia
import co.urbanmovil.app.domain.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModeloRutasTest {

    private val horaFija = 1_767_268_800_000L // 2026-01-01T12:00Z = 7:00:00 a. m. en Bogotá
    private val aqui = Lugar("Mi ubicación", Punto(0.0, 0.0))
    private val r1 = Fixtures.ruta("R1", Fixtures.recta, alternos = listOf(Fixtures.recta.take(3)))
    private val r2 = Fixtures.ruta("R2", Fixtures.recta)

    private val estados = listOf(
        Fixtures.estado("R1", 0.0, Vigencia.RECIENTE, busId = "A").copy(horaUltimaUbicacion = horaFija),
        Fixtures.estado("R1", 0.0, Vigencia.DESACTUALIZADA, busId = "B").copy(horaUltimaUbicacion = horaFija - 90_000),
        Fixtures.estado("R2", 0.0, Vigencia.OCULTA, busId = "C").copy(horaUltimaUbicacion = horaFija - 20 * 60_000),
    )

    @Test
    fun rutasUi_cuentaBusesActivosYOcultaLosOcultos() {
        val ui = construirRutasUi(listOf(r1, r2), estados, horaFija, "R1", null, aqui)
        assertEquals(listOf(2, 0), ui.rutas.map { it.busesActivos })
        assertEquals(listOf("A", "B"), ui.buses.map { it.id })
    }

    @Test
    fun rutasUi_repositorioVacio_noTieneRutasNiSeleccion() {
        // CA-18.3: base de la pantalla «No hay rutas disponibles».
        val ui = construirRutasUi(emptyList(), emptyList(), horaFija, "R1", null, aqui)
        assertTrue(ui.rutas.isEmpty())
        assertNull(ui.seleccionadaId)
    }

    @Test
    fun rutasUi_busSeleccionadoOculto_noAbreTarjeta() {
        val ui = construirRutasUi(listOf(r1, r2), estados, horaFija, "R1", "C", aqui)
        assertNull(ui.busSeleccionado)
    }

    @Test
    fun busUi_muestraHoraDeColombiaYEdad() {
        val ui = construirDetalleRutaUi(r1, estados, horaFija, "B")
        val b = ui.busSeleccionado!!
        assertTrue(b.desactualizado)
        assertEquals("6:58:30 a. m.", b.horaUltimaUbicacion)
        assertEquals("hace 1 min", b.edadUbicacion)
    }

    @Test
    fun detalle_rutaSinBusesVisibles_listaVacia() {
        // CA-19.4
        assertTrue(construirDetalleRutaUi(r2, estados, horaFija, null).buses.isEmpty())
    }

    @Test
    fun detalle_rutaInexistente_esNull() {
        val ui = construirDetalleRutaUi(null, estados, horaFija, null)
        assertNull(ui.ruta)
        assertTrue(ui.buses.isEmpty())
    }

    @Test
    fun lineasDeRuta_principalSolidaYAlternosPunteados() {
        val lineas = lineasDeRuta(r1)
        assertEquals(listOf(false, true), lineas.map { it.punteada })
        assertTrue(lineas[0].ancho > lineas[1].ancho)
    }

    @Test
    fun lineasDeRutas_seleccionadaVaEncimaConSusAlternos() {
        val lineas = lineasDeRutas(listOf(r1, r2), "R1")
        assertEquals(listOf("gris-R2", "principal-R1", "alterno-R1-0"), lineas.map { it.id })
    }

    @Test
    fun demo_um221_seVeDesactualizadoEnSuVentana() {
        // UM-221: desfase 12, vuelta 24, sin señal min 10..13. En t ≡ 0 (mod vuelta) está en el minuto 12:
        // lleva 2 min congelado → DESACTUALIZADA, y su ocupación (reporte de 14 min) no se muestra.
        val vueltaMs = FakeData.R1.minutosPorVuelta * 60_000L
        val repo = FakeTransitRepository(reloj = { vueltaMs * 1_000 })
        val ui = construirDetalleRutaUi(FakeData.R1, repo.estadosEn(repo.ahoraMs()), repo.ahoraMs(), "UM-221")
        val bus = ui.busSeleccionado!!
        assertTrue(bus.desactualizado)
        assertEquals("hace 2 min", bus.edadUbicacion)
    }
}
