package co.urbanmovil.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HoraTest {

    /** 2026-01-01T12:00:00Z = 07:00:00 en America/Bogota (UTC−5). */
    private val mediodiaUtc = 1_767_268_800_000L
    private val hora = 3_600_000L
    private val minuto = 60_000L
    private val segundo = 1_000L

    @Test
    fun formatearHora_usaHoraDeColombia() {
        assertEquals("07:00", formatearHora(mediodiaUtc))
    }

    @Test
    fun formatearHora12_manana() = assertEquals("7:00:00 a. m.", formatearHora12(mediodiaUtc))

    @Test
    fun formatearHora12_noche_conSegundos() =
        assertEquals("7:42:18 p. m.", formatearHora12(mediodiaUtc + 12 * hora + 42 * minuto + 18 * segundo))

    @Test
    fun formatearHora12_medianoche_es12am() = assertEquals("12:00:00 a. m.", formatearHora12(mediodiaUtc - 7 * hora))

    @Test
    fun formatearHora12_mediodia_es12pm() = assertEquals("12:00:00 p. m.", formatearHora12(mediodiaUtc + 5 * hora))

    @Test fun formatearEdad_segundos() = assertEquals("hace 12 s", formatearEdad(12 * segundo + 400))

    @Test fun formatearEdad_59s_siguenSiendoSegundos() = assertEquals("hace 59 s", formatearEdad(59 * segundo))

    @Test fun formatearEdad_minutos() = assertEquals("hace 3 min", formatearEdad(3 * minuto + 20 * segundo))

    @Test fun formatearEdad_negativa_esCero() = assertEquals("hace 0 s", formatearEdad(-5 * segundo))
}
