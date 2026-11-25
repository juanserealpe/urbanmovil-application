package co.urbanmovil.api.reasignacion

import co.urbanmovil.api.administrador.Administrador
import co.urbanmovil.api.administrador.AdministradorRepository
import co.urbanmovil.api.common.NoAutenticadoException
import co.urbanmovil.api.common.ReglaNegocioException
import co.urbanmovil.api.common.ValidacionException
import co.urbanmovil.api.programacion.ProgramacionService
import co.urbanmovil.api.recorrido.RecorridoActivoConsulta
import co.urbanmovil.api.ruta.Ruta
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class ReasignacionService(
    private val reasignacionRepository: ReasignacionRepository,
    private val administradorRepository: AdministradorRepository,
    private val programacionService: ProgramacionService,
    private val recorridoActivo: RecorridoActivoConsulta,
    private val clock: Clock,
) {

    /** HU-25 desde la API: el administrador es el de la sesión. */
    @Transactional
    fun reasignar(conductorId: Long, solicitud: ReasignacionRequest, correoAdministrador: String): ReasignacionResponse {
        val administrador = administradorRepository.findByCorreoIgnoreCase(correoAdministrador)
            ?: throw NoAutenticadoException()
        return reasignar(conductorId, solicitud, administrador)
    }

    /**
     * HU-25 · CA-25.1 a CA-25.5. Todo en una transacción y solo sobre la programación de HOY
     * de este conductor: no toca a otros conductores ni recalcula nada (CA-25.4, RN-24).
     */
    @Transactional
    fun reasignar(conductorId: Long, solicitud: ReasignacionRequest, administrador: Administrador): ReasignacionResponse {
        val motivo = solicitud.motivo?.trim().orEmpty()
        if (motivo.isEmpty()) throw ValidacionException("motivo", "Escriba qué pasó") // CA-25.2
        val asignar = solicitud.rutasAsignar.orEmpty()
        val quitar = solicitud.rutasQuitar.orEmpty()
        if (asignar.isEmpty() && quitar.isEmpty()) {
            throw ValidacionException("rutasAsignar", "Indique al menos una ruta para asignar o quitar") // CA-25.5
        }

        val conductor = programacionService.buscarConductor(conductorId)
        val rutas = programacionService.buscarRutas(asignar + quitar)
        validarSinRepetidos("rutasAsignar", asignar, rutas)
        validarSinRepetidos("rutasQuitar", quitar, rutas)
        asignar.firstOrNull { it in quitar }?.let {
            throw ValidacionException("rutasQuitar", "La ruta ${rutas.getValue(it).codigo} no puede asignarse y quitarse a la vez")
        }

        val programacion = programacionService.deHoyOCrear(conductor)
        asignar.firstOrNull { programacion.tiene(it) }?.let {
            throw ReglaNegocioException("La ruta ${rutas.getValue(it).codigo} ya está en la programación")
        }
        quitar.firstOrNull { !programacion.tiene(it) }?.let {
            throw ReglaNegocioException("La ruta ${rutas.getValue(it).codigo} no está en la programación")
        }

        // Aplicar sobre la programación de hoy
        val enCurso = recorridoActivo.rutaEnCursoDelConductor(conductorId)
        asignar.forEach { programacion.agregarAlFinal(rutas.getValue(it)) } // al final, en el orden recibido
        quitar.forEach { rutaId ->
            if (rutaId == enCurso) {
                programacion.entrada(rutaId)!!.retiroPendiente = true // CA-25.3 / D-11
            } else {
                programacion.quitar(rutaId)
            }
        }
        programacion.renumerar()
        val guardada = programacionService.guardar(programacion)

        val reasignacion = Reasignacion(conductor, administrador, clock.instant(), motivo)
        asignar.forEach { reasignacion.registrar(rutas.getValue(it), TipoReasignacion.ASIGNADA) }
        quitar.forEach { reasignacion.registrar(rutas.getValue(it), TipoReasignacion.QUITADA) }
        val registrada = reasignacionRepository.saveAndFlush(reasignacion)

        return responder(registrada).copy(
            programacion = programacionService.responder(conductorId, guardada.fecha, guardada),
        )
    }

    /** Historial, más reciente primero. `fecha` filtra por el día de `fecha_hora` en Bogotá. */
    @Transactional(readOnly = true)
    fun historial(conductorId: Long?, fecha: LocalDate?): List<ReasignacionResponse> =
        reasignacionRepository.findAllByOrderByFechaHoraDescIdDesc()
            .asSequence()
            .filter { conductorId == null || it.conductor.id == conductorId }
            .filter { fecha == null || it.fechaHora.atZone(clock.zone).toLocalDate() == fecha }
            .map(::responder)
            .toList()

    private fun validarSinRepetidos(campo: String, ids: List<Long>, rutas: Map<Long, Ruta>) {
        ids.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 1 }?.let { (id, _) ->
            throw ValidacionException(campo, "La ruta ${rutas.getValue(id).codigo} está repetida")
        }
    }

    private fun responder(reasignacion: Reasignacion) = ReasignacionResponse(
        id = reasignacion.id!!,
        fechaHora = reasignacion.fechaHora.atZone(clock.zone).toOffsetDateTime(),
        conductor = PersonaResumen(reasignacion.conductor.id!!, reasignacion.conductor.nombre),
        administrador = PersonaResumen(reasignacion.administrador.id!!, reasignacion.administrador.nombre),
        motivo = reasignacion.motivo,
        rutasAsignadas = reasignacion.codigos(TipoReasignacion.ASIGNADA),
        rutasQuitadas = reasignacion.codigos(TipoReasignacion.QUITADA),
    )
}
