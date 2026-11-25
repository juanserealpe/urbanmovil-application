package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.Punto
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoTest {

    @Test
    fun distanciaM_milesimaDeGradoDeLatitud_midenCercaDe111m() {
        val d = distanciaM(Punto(2.4419, -76.6066), Punto(2.4429, -76.6066))
        assertEquals(111.2, d, 0.5)
    }

    @Test
    fun distanciaM_mismoPunto_esCero() {
        assertEquals(0.0, distanciaM(Punto(2.44, -76.61), Punto(2.44, -76.61)), 1e-9)
    }

    @Test
    fun puntoMasCercano_proyectaSobreElSegmentoYDaPosicionAcumulada() {
        val total = longitudTotalM(Fixtures.recta)
        // 0.001° al norte del punto medio de la recta.
        val proyeccion = puntoMasCercano(Punto(Fixtures.LAT + 0.001, -76.61), Fixtures.recta)

        assertEquals(Fixtures.LAT, proyeccion.punto.lat, 1e-6)
        assertEquals(-76.61, proyeccion.punto.lng, 1e-6)
        assertEquals(111.2, proyeccion.distanciaM, 0.5)
        assertEquals(total / 2, proyeccion.posicionM, 1.0)
    }

    @Test
    fun puntoMasCercano_antesDelInicio_quedaEnElInicio() {
        val proyeccion = puntoMasCercano(Punto(Fixtures.LAT, -76.63), Fixtures.recta)
        assertEquals(0.0, proyeccion.posicionM, 1e-6)
        assertEquals(Fixtures.recta.first(), proyeccion.punto)
    }

    @Test
    fun distanciaSobreRecorrido_esLaDiferenciaDePosiciones() {
        assertEquals(350.0, distanciaSobreRecorrido(desdeM = 150.0, hastaM = 500.0), 1e-9)
    }

    @Test
    fun puntoEnPosicion_interpolaPorDistanciaNoPorIndice() {
        // Primer tramo cortito (≈111 m) y segundo largo (≈2 km): la mitad de la distancia cae en el segundo tramo.
        val polilinea = listOf(Punto(2.44, -76.620), Punto(2.44, -76.619), Punto(2.44, -76.600))
        val total = longitudTotalM(polilinea)
        val mitad = puntoEnPosicion(polilinea, total / 2)
        assertTrue(abs(mitad.lng - (-76.610)) < 1e-4, "lng = ${mitad.lng}")
    }

    @Test
    fun puntoEnPosicion_fueraDeRango_seRecortaALosExtremos() {
        assertEquals(Fixtures.recta.first(), puntoEnPosicion(Fixtures.recta, -10.0))
        assertEquals(Fixtures.recta.last(), puntoEnPosicion(Fixtures.recta, 1e9))
    }
}
