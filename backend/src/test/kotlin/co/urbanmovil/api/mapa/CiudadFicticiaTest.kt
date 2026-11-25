package co.urbanmovil.api.mapa

import co.urbanmovil.api.demo.RutasDemo
import co.urbanmovil.api.mapa.CiudadFicticia.esquina
import co.urbanmovil.api.ruta.Punto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pruebas puras: la ciudad es una función, sin Spring ni BD. */
class CiudadFicticiaTest {

    @Test
    fun `las esquinas estan en una cuadricula regular centrada en el parque central`() {
        assertEquals(Punto(2.444, -76.606), esquina(10, 10))
        assertEquals(Punto(2.434, -76.616), esquina(0, 0))
        assertEquals(Punto(2.454, -76.596), esquina(20, 20))
    }

    @Test
    fun `una esquina fuera de la ciudad no existe`() {
        assertThrows<IllegalArgumentException> { esquina(21, 0) }
        assertThrows<IllegalArgumentException> { esquina(0, -1) }
    }

    @Test
    fun `un trazado por una carrera pasa por todas las esquinas intermedias`() {
        assertEquals(
            listOf(esquina(5, 8), esquina(5, 9), esquina(5, 10)),
            CiudadFicticia.trazado(esquina(5, 8), esquina(5, 10)),
        )
    }

    @Test
    fun `un trazado puede ir por calles, carreras y la diagonal`() {
        val puntos = CiudadFicticia.trazado(esquina(2, 2), esquina(4, 4), esquina(4, 1), esquina(2, 1))
        assertEquals(listOf(esquina(2, 2), esquina(3, 3), esquina(4, 4), esquina(4, 3), esquina(4, 2), esquina(4, 1), esquina(3, 1), esquina(2, 1)), puntos)
    }

    @Test
    fun `un tramo que no va por ninguna calle se rechaza`() {
        // De (0,0) a (3,1): atravesaría manzanas.
        assertThrows<IllegalArgumentException> { CiudadFicticia.trazado(esquina(0, 0), esquina(3, 1)) }
        // Diagonal fuera de la avenida diagonal: tampoco es una calle.
        assertThrows<IllegalArgumentException> { CiudadFicticia.trazado(esquina(3, 1), esquina(5, 3)) }
    }

    @Test
    fun `sobre calle reconoce carreras, calles y la diagonal y rechaza el interior de una manzana`() {
        assertTrue(CiudadFicticia.estaSobreCalle(Punto(2.4385, -76.611))) // carrera 5, entre calles
        assertTrue(CiudadFicticia.estaSobreCalle(Punto(2.444, -76.6085))) // calle 10, entre carreras
        assertTrue(CiudadFicticia.estaSobreCalle(Punto(2.4395, -76.6105))) // diagonal
        assertTrue(!CiudadFicticia.estaSobreCalle(Punto(2.4385, -76.6105))) // dentro de una manzana
        assertTrue(!CiudadFicticia.estaSobreCalle(Punto(2.50, -76.611))) // fuera de la ciudad
    }

    // ---------- Las rutas de demostración tienen sentido ----------

    @Test
    fun `todos los puntos de todas las rutas de demo y sus alternos van por calles`() {
        RutasDemo.rutas.forEach { ruta ->
            (ruta.recorrido + ruta.alternos.flatMap { it.puntos }).forEach { p ->
                assertTrue(CiudadFicticia.estaSobreCalle(p), "${ruta.codigo}: $p no está sobre una calle")
            }
            // Y también los tramos: el punto medio de cada par consecutivo cae en la misma calle.
            (listOf(ruta.recorrido) + ruta.alternos.map { it.puntos }).forEach { trazado ->
                trazado.zipWithNext().forEach { (a, b) ->
                    val medio = Punto((a.lat + b.lat) / 2, (a.lng + b.lng) / 2)
                    assertTrue(CiudadFicticia.estaSobreCalle(medio), "${ruta.codigo}: el tramo $a → $b cruza una manzana")
                }
            }
        }
    }

    @Test
    fun `cada recorrido principal es un circuito cerrado (el bus no salta al reiniciar la vuelta)`() {
        RutasDemo.rutas.forEach { assertEquals(it.recorrido.first(), it.recorrido.last(), it.codigo) }
    }

    @Test
    fun `cada alterno sale y vuelve a su recorrido principal`() {
        RutasDemo.rutas.forEach { ruta ->
            ruta.alternos.forEach { alterno ->
                assertTrue(alterno.puntos.first() in ruta.recorrido, "${ruta.codigo}/${alterno.nombre} no sale del principal")
                assertTrue(alterno.puntos.last() in ruta.recorrido, "${ruta.codigo}/${alterno.nombre} no vuelve al principal")
            }
        }
    }

    @Test
    fun `los lugares y la ubicacion del pasajero estan sobre calles`() {
        (CiudadFicticia.lugares + CiudadFicticia.ubicacionPasajero).forEach {
            assertTrue(CiudadFicticia.estaSobreCalle(it.punto), "${it.nombre} no está sobre una calle")
        }
    }

    // ---------- Lo que se dibuja ----------

    @Test
    fun `el geojson trae calles, avenidas con nombre, manzanas, parques y el rio`() {
        val tipos = CiudadFicticia.geoJson().features.groupBy { it.properties["tipo"] }
        assertEquals(21 * 2 - 10, tipos.getValue("calle").size) // 21 carreras + 21 calles − 10 avenidas
        assertEquals(11, tipos.getValue("avenida").size) // 5 carreras + 5 calles + la diagonal
        assertTrue(tipos.getValue("avenida").all { (it.properties["nombre"] as String).isNotBlank() })
        assertTrue(tipos.getValue("manzana").size > 300)
        assertTrue(tipos.getValue("parque").isNotEmpty())
        assertEquals(1, tipos.getValue("rio").size)
    }
}
