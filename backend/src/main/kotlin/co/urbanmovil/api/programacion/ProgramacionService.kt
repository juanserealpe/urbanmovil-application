package co.urbanmovil.api.programacion

import co.urbanmovil.api.common.NoEncontradoException
import co.urbanmovil.api.common.ReglaNegocioException
import co.urbanmovil.api.common.ValidacionException
import co.urbanmovil.api.conductor.Conductor
import co.urbanmovil.api.conductor.ConductorRepository
import co.urbanmovil.api.recorrido.RecorridoActivoConsulta
import co.urbanmovil.api.ruta.Ruta
import co.urbanmovil.api.ruta.RutaRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ProgramacionService(
    private val programacionRepository: ProgramacionRepository,
    private val conductorRepository: ConductorRepository,
    private val rutaRepository: RutaRepository,
    private val recorridoActivo: RecorridoActivoConsulta,
    private val consulta: ProgramacionConsulta,
) {

    /** HU-09: programación del conductor en una fecha (por defecto hoy). */
    @Transactional(readOnly = true)
    fun obtener(conductorId: Long, fecha: LocalDate?): ProgramacionResponse {
        val conductor = buscarConductor(conductorId)
        val dia = fecha ?: consulta.hoy()
        return responder(conductor.id!!, dia, programacionRepository.findByConductorIdAndFecha(conductorId, dia))
    }

    /**
     * HU-09 · CA-09.1 a CA-09.5. `rutasIds` es la lista completa y ordenada.
     * Conserva `retiro_pendiente` de las rutas que siguen. Si algo es inválido no cambia nada.
     */
    @Transactional
    fun reemplazar(conductorId: Long, fecha: LocalDate?, rutasIds: List<Long>): ProgramacionResponse {
        val conductor = buscarConductor(conductorId)
        val hoy = consulta.hoy()
        val dia = fecha ?: hoy
        if (dia.isBefore(hoy)) throw ReglaNegocioException("No se puede modificar la programación de un día pasado")

        val rutas = buscarRutas(rutasIds)
        rutasIds.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.let { (id, _) ->
            throw ValidacionException("rutas", "La ruta ${rutas.getValue(id).codigo} está repetida en la programación") // CA-09.4
        }

        val existente = programacionRepository.findByConductorIdAndFecha(conductorId, dia)

        // CA-09.3: no se retira una ruta que el conductor está recorriendo.
        if (dia == hoy && existente != null) {
            val enCurso = recorridoActivo.rutaEnCursoDelConductor(conductorId)
            if (enCurso != null && existente.tiene(enCurso) && enCurso !in rutasIds) {
                val codigo = existente.entrada(enCurso)!!.ruta.codigo
                throw ReglaNegocioException(
                    "No se puede retirar la ruta $codigo: el conductor la está recorriendo. Se podrá retirar cuando termine.",
                )
            }
        }

        if (existente == null && rutasIds.isEmpty()) return responder(conductorId, dia, null)

        val programacion = existente ?: Programacion(conductor, dia)
        val actuales = programacion.rutas.associateBy { it.ruta.id!! }
        val conservar: Set<Long?> = rutasIds.toSet()
        programacion.rutas.removeIf { it.ruta.id !in conservar } // CA-09.2
        rutasIds.forEachIndexed { i, rutaId ->
            val entrada = actuales[rutaId]
            if (entrada != null) {
                entrada.orden = i + 1 // CA-09.5 (retiro_pendiente se conserva)
            } else {
                programacion.rutas.add(ProgramacionRuta(programacion, rutas.getValue(rutaId), orden = i + 1))
            }
        }
        return responder(conductorId, dia, programacionRepository.saveAndFlush(programacion))
    }

    /** Resumen del día para la pantalla de programación: todos los conductores. */
    @Transactional(readOnly = true)
    fun resumenDelDia(fecha: LocalDate?): List<ProgramacionResumenResponse> {
        val dia = fecha ?: consulta.hoy()
        val rutasPorConductor = consulta.rutasPorConductor(dia)
        val enRecorrido = recorridoActivo.porConductor()
        return conductorRepository.findAll(Sort.by("nombre")).map {
            ProgramacionResumenResponse(
                conductorId = it.id!!,
                nombre = it.nombre,
                disponible = it.disponible,
                enRecorrido = enRecorrido[it.id],
                totalRutas = rutasPorConductor[it.id]?.size ?: 0,
            )
        }
    }

    // ------------------------------------------------------------------ Para otros servicios

    /** Programación de hoy del conductor, o una nueva sin guardar todavía. */
    fun deHoyOCrear(conductor: Conductor): Programacion =
        programacionRepository.findByConductorIdAndFecha(conductor.id!!, consulta.hoy()) ?: Programacion(conductor, consulta.hoy())

    fun deHoy(conductorId: Long): Programacion? =
        programacionRepository.findByConductorIdAndFecha(conductorId, consulta.hoy())

    fun guardar(programacion: Programacion): Programacion = programacionRepository.saveAndFlush(programacion)

    /** D-11: al finalizar un recorrido, la ruta con retiro pendiente sale de la programación de hoy. */
    fun aplicarRetiroPendiente(conductorId: Long, rutaId: Long) {
        val programacion = deHoy(conductorId) ?: return
        if (programacion.entrada(rutaId)?.retiroPendiente == true) {
            programacion.quitar(rutaId)
            programacionRepository.saveAndFlush(programacion)
        }
    }

    fun responder(conductorId: Long, fecha: LocalDate, programacion: Programacion?): ProgramacionResponse {
        val enCurso = if (fecha == consulta.hoy()) recorridoActivo.rutaEnCursoDelConductor(conductorId) else null
        val rutas = programacion?.ordenadas().orEmpty().map { entrada ->
            val ruta = entrada.ruta
            RutaProgramadaResponse(
                rutaId = ruta.id!!,
                codigo = ruta.codigo,
                nombre = ruta.nombre,
                color = ruta.color,
                orden = entrada.orden,
                estado = when {
                    entrada.retiroPendiente -> EstadoRutaProgramada.RETIRO_PENDIENTE
                    ruta.id == enCurso -> EstadoRutaProgramada.EN_RECORRIDO
                    else -> EstadoRutaProgramada.PENDIENTE
                },
            )
        }
        return ProgramacionResponse(conductorId, fecha, rutas)
    }

    fun buscarConductor(id: Long): Conductor =
        conductorRepository.findById(id).orElseThrow { NoEncontradoException("El conductor no existe") }

    /** Carga las rutas pedidas; 404 si alguna no existe. */
    fun buscarRutas(ids: Collection<Long>): Map<Long, Ruta> {
        val encontradas = rutaRepository.findAllById(ids.toSet()).associateBy { it.id!! }
        ids.firstOrNull { it !in encontradas }?.let { throw NoEncontradoException("No existe la ruta con id $it") }
        return encontradas
    }
}
