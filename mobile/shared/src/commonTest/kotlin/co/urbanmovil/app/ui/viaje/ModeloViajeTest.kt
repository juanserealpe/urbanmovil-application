package co.urbanmovil.app.ui.viaje

import co.urbanmovil.app.data.FakeData
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.domain.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModeloViajeTest {

    /** ≈ 55 m al sur del punto medio de la recta (posición ≈ 1 112 m). */
    private val p = Punto(Fixtures.LAT - 0.0005, -76.61)

    // ------------------------------------------------------------ ¿Cuándo pasa?

    @Test
    fun tarjetas_ordenTiempoAscendente_luegoSinBus_luegoLejos() {
        val lejos = Fixtures.ruta("D", Fixtures.recta.map { it.copy(lat = it.lat + 0.01) }) // ≈ 1.1 km
        val yaPaso = Fixtures.ruta("C", Fixtures.recta)
        val lenta = Fixtures.ruta("A", Fixtures.recta)
        val rapida = Fixtures.ruta("B", Fixtures.recta)
        val estados = listOf(
            Fixtures.estado("A", 0.0), // ≈ 1 112 m → 4 min a 300 m/min
            Fixtures.estado("B", 800.0), // ≈ 312 m → 2 min
            Fixtures.estado("C", 1_500.0), // ya pasó
        )

        val tarjetas = construirTarjetasEta(p, listOf(lejos, yaPaso, lenta, rapida), estados, emptyList())

        assertEquals(listOf("B", "A", "C", "D"), tarjetas.map { it.ruta.id })
        assertEquals(EstadoEtaUi.SinBus(busesRecientes = 1), tarjetas[2].estado)
        assertEquals(EstadoEtaUi.Lejos, tarjetas[3].estado)
    }

    @Test
    fun tarjeta_llega_traeDistanciaYPuntoParaElMapa() {
        val t = construirTarjetasEta(p, listOf(Fixtures.rutaRecta), listOf(Fixtures.estado("RT", 0.0)), emptyList()).single()
        val llega = assertIs<EstadoEtaUi.Llega>(t.estado)
        assertEquals(56, llega.distanciaM, "≈ 55 m de caminata")
        assertNull(llega.lugar)
        assertNotNull(t.proyeccion)
    }

    @Test
    fun tarjeta_nombraElLugarCercanoAlPuntoDelRecorrido() {
        val parque = Lugar("Parque de prueba", Punto(Fixtures.LAT, -76.61))
        val t = construirTarjetasEta(p, listOf(Fixtures.rutaRecta), listOf(Fixtures.estado("RT", 0.0)), listOf(parque)).single()
        assertEquals("Parque de prueba", assertIs<EstadoEtaUi.Llega>(t.estado).lugar)
    }

    @Test
    fun tarjeta_sinBusesRecientes_explicaQueNoHayUbicacionReciente() {
        // CA-20.3
        val t = construirTarjetasEta(p, listOf(Fixtures.rutaRecta), emptyList(), emptyList()).single()
        assertEquals(EstadoEtaUi.SinBus(busesRecientes = 0), t.estado)
        assertNotNull(t.proyeccion, "Está a ≤ 500 m: se dibuja el punto del recorrido")
    }

    @Test
    fun tarjeta_lejos_noTieneProyeccion() {
        // CA-20.4
        val lejos = Punto(Fixtures.LAT + 0.01, -76.61)
        val t = construirTarjetasEta(lejos, listOf(Fixtures.rutaRecta), listOf(Fixtures.estado("RT", 0.0)), emptyList()).single()
        assertEquals(EstadoEtaUi.Lejos, t.estado)
        assertNull(t.proyeccion)
    }

    // ------------------------------------------------------------ Buscar viaje

    private val origen = PuntoElegido.EnMapa(Punto(Fixtures.LAT, -76.618))
    private val destino = PuntoElegido.EnMapa(Punto(Fixtures.LAT, -76.60))

    @Test
    fun demo_miUbicacionATulcan_dosOpciones_soloLaPrimeraEsMenosCaminata() {
        val tulcan = FakeData.lugares.first { it.nombre.contains("Tulcán") }
        val r = construirResultadoBusqueda(
            PuntoElegido.MiUbicacion(FakeData.MI_UBICACION.punto),
            PuntoElegido.DeLugar(tulcan),
            FakeData.rutas,
            emptyList(),
            FakeData.lugares,
        )
        val opciones = assertIs<ResultadoBusquedaUi.Opciones>(r).opciones
        assertEquals(2, opciones.size, "Opciones: ${opciones.map { it.id }}")
        assertEquals(listOf(true, false), opciones.map { it.menosCaminata })
    }

    @Test
    fun puntosIguales_pideDistintos() {
        // CA-21.5
        assertEquals(
            ResultadoBusquedaUi.PuntosIguales,
            construirResultadoBusqueda(origen, origen, listOf(Fixtures.rutaRecta), emptyList(), emptyList()),
        )
    }

    @Test
    fun ningunaRuta_listaVacia() {
        // CA-21.4
        val lejana = Fixtures.ruta("LEJANA", Fixtures.recta.map { it.copy(lat = it.lat + 0.02) })
        val r = assertIs<ResultadoBusquedaUi.Opciones>(construirResultadoBusqueda(origen, destino, listOf(lejana), emptyList(), emptyList()))
        assertTrue(r.opciones.isEmpty())
    }

    @Test
    fun sinBusProximo_conservaElTrayecto() {
        // CA-22.3
        val opcion = assertIs<ResultadoBusquedaUi.Opciones>(
            construirResultadoBusqueda(origen, destino, listOf(Fixtures.rutaRecta), emptyList(), emptyList()),
        ).opciones.single()
        assertNull(opcion.etaMin)
        assertTrue(opcion.trayectoMin > 0)
    }

    @Test
    fun losTiemposNoCambianElOrden() {
        // CA-22.4: la más cercana va primero aunque no tenga bus y la otra sí.
        val cercana = Fixtures.ruta("CERCANA", Fixtures.recta.map { it.copy(lat = it.lat + 0.0005) })
        val lejana = Fixtures.ruta("LEJANA", Fixtures.recta.map { it.copy(lat = it.lat + 0.002) })
        val opciones = assertIs<ResultadoBusquedaUi.Opciones>(
            construirResultadoBusqueda(origen, destino, listOf(lejana, cercana), listOf(Fixtures.estado("LEJANA", 0.0)), emptyList()),
        ).opciones
        assertEquals(listOf("CERCANA", "LEJANA"), opciones.map { it.id })
        assertNull(opciones[0].etaMin)
        assertNotNull(opciones[1].etaMin)
    }
}
