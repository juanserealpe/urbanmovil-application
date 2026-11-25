package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.NivelOcupacion
import co.urbanmovil.app.data.model.Vigencia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FreshnessTest {

    private val s = 1_000L
    private val min = 60_000L

    @Test fun cero_segundos_esReciente() = assertEquals(Vigencia.RECIENTE, calcularVigencia(0))

    @Test fun sesenta_segundos_esReciente() = assertEquals(Vigencia.RECIENTE, calcularVigencia(60 * s))

    @Test fun sesentaYUno_segundos_esDesactualizada() =
        assertEquals(Vigencia.DESACTUALIZADA, calcularVigencia(61 * s))

    @Test fun quince_minutos_esDesactualizada() =
        assertEquals(Vigencia.DESACTUALIZADA, calcularVigencia(15 * min))

    @Test fun quinceMinutosYUnSegundo_esOculta() =
        assertEquals(Vigencia.OCULTA, calcularVigencia(15 * min + s))

    @Test
    fun ocupacion_reporteViejo_noSeMuestra() {
        val bus = Bus("X", "R", "c", 0, ocupacion = NivelOcupacion.LLENO, ocupacionReportadaHaceMin = 11)
        assertNull(ocupacionVigente(bus))
    }

    @Test
    fun ocupacion_reporteReciente_seMuestra() {
        val bus = Bus("X", "R", "c", 0, ocupacion = NivelOcupacion.LLENO, ocupacionReportadaHaceMin = 10)
        assertEquals(NivelOcupacion.LLENO, ocupacionVigente(bus))
    }

    @Test
    fun infoOcupacion_reporteReciente_esVigenteConAntiguedad() {
        val bus = Bus("X", "R", "c", 0, ocupacion = NivelOcupacion.SOLO_DE_PIE, ocupacionReportadaHaceMin = 4)
        assertEquals(InfoOcupacion.Vigente(NivelOcupacion.SOLO_DE_PIE, 4), infoOcupacion(bus, mostrar = true))
    }

    @Test
    fun infoOcupacion_reporteViejo_esSinInformacion() {
        val bus = Bus("X", "R", "c", 0, ocupacion = NivelOcupacion.LLENO, ocupacionReportadaHaceMin = 11)
        assertEquals(InfoOcupacion.SinInformacion, infoOcupacion(bus, mostrar = true))
    }

    @Test
    fun infoOcupacion_sinReportes_esSinInformacion() {
        val bus = Bus("X", "R", "c", 0)
        assertEquals(InfoOcupacion.SinInformacion, infoOcupacion(bus, mostrar = true))
    }

    @Test
    fun infoOcupacion_banderaApagada_esOculta() {
        val bus = Bus("X", "R", "c", 0, ocupacion = NivelOcupacion.LLENO, ocupacionReportadaHaceMin = 1)
        assertEquals(InfoOcupacion.Oculta, infoOcupacion(bus, mostrar = false))
    }

    @Test
    fun ocupacion_banderaApagada_noSeMuestra() {
        val bus = Bus("X", "R", "c", 0, ocupacion = NivelOcupacion.LLENO, ocupacionReportadaHaceMin = 1)
        assertNull(ocupacionVigente(bus, mostrar = false))
    }
}
