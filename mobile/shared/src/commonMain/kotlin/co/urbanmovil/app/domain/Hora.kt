package co.urbanmovil.app.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Popayán: UTC−5 todo el año. */
val ZONA_HORARIA_APP: TimeZone = TimeZone.of("America/Bogota")

/** "HH:mm" en hora de Colombia, p. ej. para «Última ubicación 07:42». */
@OptIn(ExperimentalTime::class)
fun formatearHora(epochMs: Long, zona: TimeZone = ZONA_HORARIA_APP): String {
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zona)
    return "${local.hour.dos()}:${local.minute.dos()}"
}

/**
 * Hora de Colombia en formato de 12 horas con segundos, p. ej. «7:42:18 p. m.» (HU-19).
 * Medianoche es «12:… a. m.» y mediodía «12:… p. m.».
 */
@OptIn(ExperimentalTime::class)
fun formatearHora12(epochMs: Long, zona: TimeZone = ZONA_HORARIA_APP): String {
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zona)
    val hora12 = (local.hour % 12).let { if (it == 0) 12 else it }
    val sufijo = if (local.hour < 12) "a. m." else "p. m."
    return "$hora12:${local.minute.dos()}:${local.second.dos()} $sufijo"
}

/**
 * Antigüedad de un dato en texto corto: «hace 12 s» por debajo del minuto, «hace 3 min» desde ahí.
 * Una edad negativa (reloj desfasado) se trata como cero.
 */
fun formatearEdad(edadMs: Long): String {
    val segundos = edadMs.coerceAtLeast(0) / 1_000
    return if (segundos < 60) "hace $segundos s" else "hace ${segundos / 60} min"
}

private fun Int.dos(): String = toString().padStart(2, '0')
