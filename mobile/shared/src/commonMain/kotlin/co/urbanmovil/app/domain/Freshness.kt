package co.urbanmovil.app.domain

import co.urbanmovil.app.data.FakeData
import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.NivelOcupacion
import co.urbanmovil.app.data.model.Vigencia

/**
 * RN-07 / HU-19. Vigencia según la edad de la última ubicación:
 * - ≤ 60 s → RECIENTE
 * - > 60 s y ≤ 15 min → DESACTUALIZADA
 * - > 15 min → OCULTA
 */
fun calcularVigencia(
    edadMs: Long,
    recienteS: Int = FakeData.VIGENCIA_RECIENTE_S,
    ocultaMin: Int = FakeData.VIGENCIA_OCULTA_MIN,
): Vigencia = when {
    edadMs <= recienteS * 1_000L -> Vigencia.RECIENTE
    edadMs <= ocultaMin * 60_000L -> Vigencia.DESACTUALIZADA
    else -> Vigencia.OCULTA
}

/**
 * HU-24 / CA-24.2. Ocupación que se puede mostrar, o `null` si la bandera está apagada (D-01),
 * no hay reporte o el reporte es más viejo que [vigenteMin].
 */
fun ocupacionVigente(
    bus: Bus,
    vigenteMin: Int = FakeData.OCUPACION_VIGENTE_MIN,
    mostrar: Boolean = FakeData.MOSTRAR_OCUPACION,
): NivelOcupacion? {
    if (!mostrar) return null
    val hace = bus.ocupacionReportadaHaceMin ?: return null
    return if (hace <= vigenteMin) bus.ocupacion else null
}

/**
 * Qué mostrar de ocupación para un bus (HU-24). A diferencia de [ocupacionVigente], distingue
 * «la función está apagada» (no se pinta nada, D-01) de «no hay dato» (CA-24.2, CA-24.3).
 */
sealed interface InfoOcupacion {
    /** Bandera `MOSTRAR_OCUPACION` apagada: ningún elemento de ocupación en pantalla. */
    data object Oculta : InfoOcupacion

    /** Sin reportes o reporte vencido: «Sin información de ocupación». */
    data object SinInformacion : InfoOcupacion

    /** CA-24.1: nivel, antigüedad y aviso de dato aproximado. */
    data class Vigente(val nivel: NivelOcupacion, val haceMin: Int) : InfoOcupacion
}

fun infoOcupacion(
    bus: Bus,
    vigenteMin: Int = FakeData.OCUPACION_VIGENTE_MIN,
    mostrar: Boolean = FakeData.MOSTRAR_OCUPACION,
): InfoOcupacion {
    if (!mostrar) return InfoOcupacion.Oculta
    val nivel = ocupacionVigente(bus, vigenteMin, mostrar) ?: return InfoOcupacion.SinInformacion
    return InfoOcupacion.Vigente(nivel, bus.ocupacionReportadaHaceMin ?: 0)
}
