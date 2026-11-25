package co.urbanmovil.api.programacion

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

/**
 * Lecturas de la programación del día para otras pantallas:
 * `rutasHoy` en conductores y `conductoresHoy` en rutas (§7 de la Fase 3).
 */
@Component
class ProgramacionConsulta(
    private val programacionRutaRepository: ProgramacionRutaRepository,
    private val clock: Clock,
) {

    /** «Hoy» siempre en America/Bogota según el Clock. */
    fun hoy(): LocalDate = LocalDate.now(clock)

    /** Códigos de ruta por conductor, en el orden de la programación. */
    fun rutasPorConductor(fecha: LocalDate = hoy()): Map<Long, List<String>> =
        programacionRutaRepository.filasDelDia(fecha)
            .groupBy({ it.conductorId }, { it.rutaCodigo })

    /** Nombres de los conductores que tienen cada ruta en su programación (orden alfabético). */
    fun conductoresPorRuta(fecha: LocalDate = hoy()): Map<Long, List<String>> =
        programacionRutaRepository.filasDelDia(fecha)
            .groupBy({ it.rutaId }, { it.conductorNombre })
            .mapValues { (_, nombres) -> nombres.distinct().sorted() }
}
