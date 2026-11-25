package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Vigencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EtaCalculatorTest {

    private val ruta = Fixtures.rutaRecta

    /** Paradero a ~55 m al sur del punto medio de la recta. */
    private val paradero = Punto(Fixtures.LAT - 0.0005, -76.61)
    private val objetivoM = puntoMasCercano(paradero, ruta.principal).posicionM

    private fun eta(vararg estados: co.urbanmovil.app.data.model.EstadoBus) =
        calcularEta(paradero, ruta, estados.toList(), velocidadKmh = Fixtures.VELOCIDAD_KMH)

    @Test
    fun puntoAMasDe500m_esLejosDeLaRuta() {
        val lejos = Punto(Fixtures.LAT + 0.01, -76.61) // ≈ 1.1 km
        val resultado = calcularEta(lejos, ruta, listOf(Fixtures.estado("RT", 0.0)))
        assertEquals(ResultadoEta.LejosDeLaRuta, resultado)
    }

    @Test
    fun busQueYaPaso_esSinBusProximo() {
        assertEquals(ResultadoEta.SinBusProximo, eta(Fixtures.estado("RT", objetivoM + 300)))
    }

    @Test
    fun busDesactualizado_noCuenta() {
        val desactualizado = Fixtures.estado("RT", objetivoM - 300, Vigencia.DESACTUALIZADA)
        assertEquals(ResultadoEta.SinBusProximo, eta(desactualizado))
    }

    @Test
    fun busOculto_noCuenta() {
        assertEquals(ResultadoEta.SinBusProximo, eta(Fixtures.estado("RT", objetivoM - 300, Vigencia.OCULTA)))
    }

    @Test
    fun busDeOtraRuta_noCuenta() {
        assertEquals(ResultadoEta.SinBusProximo, eta(Fixtures.estado("OTRA", objetivoM - 300)))
    }

    @Test
    fun casoFeliz_devuelveMinutosPositivosDelBusMasCercanoAguasArriba() {
        val resultado = eta(
            Fixtures.estado("RT", objetivoM - 1_200, busId = "LEJANO"),
            Fixtures.estado("RT", objetivoM - 450, busId = "CERCANO"), // 450 m a 300 m/min = 1.5 → 2
            Fixtures.estado("RT", objetivoM + 100, busId = "YA_PASO"),
        )
        val llega = assertIs<ResultadoEta.Llega>(resultado)
        assertTrue(llega.minutos > 0)
        assertEquals(2, llega.minutos)
        assertEquals("CERCANO", llega.busId)
        assertEquals(objetivoM, llega.puntoDelRecorrido.posicionM, 1e-9)
    }

    @Test
    fun busEncimaDelParadero_esMinimoUnMinuto() {
        val llega = assertIs<ResultadoEta.Llega>(eta(Fixtures.estado("RT", objetivoM)))
        assertEquals(1, llega.minutos)
    }

    @Test
    fun tiempoTrayecto_redondeaHaciaArriba() {
        assertEquals(3, tiempoTrayecto(abordajeM = 100.0, descensoM = 1_000.0, velocidadKmh = 18.0))
        assertEquals(4, tiempoTrayecto(abordajeM = 100.0, descensoM = 1_001.0, velocidadKmh = 18.0))
    }
}
