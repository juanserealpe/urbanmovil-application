package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Vigencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Fase3DominioTest {

    @Test
    fun tramoEntre_empiezaYTerminaEnLasPosicionesYMideLaDiferencia() {
        val tramo = tramoEntre(Fixtures.recta, 100.0, 900.0)
        assertEquals(puntoEnPosicion(Fixtures.recta, 100.0), tramo.first())
        assertEquals(puntoEnPosicion(Fixtures.recta, 900.0), tramo.last())
        assertEquals(800.0, longitudTotalM(tramo), 1.0)
    }

    @Test
    fun tramoEntre_conservaLosVerticesIntermedios() {
        // Cada vértice de la recta está a ≈ 222 m: entre 100 y 900 m caben los vértices 1..4.
        assertEquals(6, tramoEntre(Fixtures.recta, 100.0, 900.0).size)
    }

    @Test
    fun tramoEntre_alReves_falla() {
        assertFailsWith<IllegalArgumentException> { tramoEntre(Fixtures.recta, 500.0, 100.0) }
    }

    @Test
    fun lugarMasCercano_eligeElMasProximoDentroDelRadio() {
        val cerca = Lugar("Cerca", Punto(Fixtures.LAT + 0.0005, -76.62)) // ≈ 55 m
        val masCerca = Lugar("Más cerca", Punto(Fixtures.LAT + 0.0002, -76.62)) // ≈ 22 m
        assertEquals(masCerca, lugarMasCercano(Punto(Fixtures.LAT, -76.62), listOf(cerca, masCerca)))
    }

    @Test
    fun lugarMasCercano_fueraDelRadio_esNull() {
        val lejos = Lugar("Lejos", Punto(Fixtures.LAT + 0.01, -76.62)) // ≈ 1.1 km
        assertNull(lugarMasCercano(Punto(Fixtures.LAT, -76.62), listOf(lejos)))
    }

    @Test
    fun contarBusesRecientes_ignoraDesactualizadosYOtrasRutas() {
        val estados = listOf(
            Fixtures.estado("R1", 0.0, Vigencia.RECIENTE, busId = "A"),
            Fixtures.estado("R1", 0.0, Vigencia.DESACTUALIZADA, busId = "B"),
            Fixtures.estado("R2", 0.0, Vigencia.RECIENTE, busId = "C"),
        )
        assertEquals(1, contarBusesRecientes(estados, "R1"))
        assertTrue(contarBusesRecientes(estados, "R3") == 0)
    }
}
