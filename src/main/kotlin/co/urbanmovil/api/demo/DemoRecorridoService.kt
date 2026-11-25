package co.urbanmovil.api.demo

import co.urbanmovil.api.common.NoEncontradoException
import co.urbanmovil.api.common.ReglaNegocioException
import co.urbanmovil.api.conductor.ConductorRepository
import co.urbanmovil.api.programacion.ProgramacionService
import co.urbanmovil.api.recorrido.EstadoRecorrido
import co.urbanmovil.api.recorrido.Recorrido
import co.urbanmovil.api.recorrido.RecorridoActivoConsulta
import co.urbanmovil.api.recorrido.RecorridoRepository
import co.urbanmovil.api.ruta.RutaRepository
import co.urbanmovil.api.vehiculo.VehiculoRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * D-16: como la app del conductor no existe, los recorridos se inician y finalizan
 * desde aquí. Solo existe con `urbanmovil.demo.habilitado=true`.
 */
@Service
@ConditionalOnProperty(prefix = "urbanmovil.demo", name = ["habilitado"], havingValue = "true")
class DemoRecorridoService(
    private val recorridoRepository: RecorridoRepository,
    private val conductorRepository: ConductorRepository,
    private val rutaRepository: RutaRepository,
    private val vehiculoRepository: VehiculoRepository,
    private val recorridoActivo: RecorridoActivoConsulta,
    private val programacionService: ProgramacionService,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun listar(estado: EstadoRecorrido?): List<RecorridoDemoResponse> =
        (if (estado == null) recorridoRepository.findTodosCompletos() else recorridoRepository.findCompletosPorEstado(estado))
            .map(::responder)

    /** Inicia un recorrido `ACTIVO` con `inicio = ahora`. */
    @Transactional
    fun iniciar(solicitud: IniciarRecorridoRequest): RecorridoDemoResponse = iniciarDesde(solicitud, clock.instant())

    /**
     * Igual que [iniciar], con una hora de inicio dada (la semilla los inicia «hace unos minutos»).
     *
     * OJO: no usar un parámetro por defecto (`inicio = clock.instant()`). Kotlin lo evalúa en un
     * método estático `iniciar$default` que lee `clock` del objeto recibido, y en un bean con
     * `@Transactional` ese objeto es el proxy de Spring, cuyos campos son null.
     */
    @Transactional
    fun iniciarDesde(solicitud: IniciarRecorridoRequest, inicio: Instant): RecorridoDemoResponse {
        val conductor = conductorRepository.findById(solicitud.conductorId!!)
            .orElseThrow { NoEncontradoException("El conductor no existe") }
        val ruta = rutaRepository.findById(solicitud.rutaId!!).orElseThrow { NoEncontradoException("La ruta no existe") }
        val vehiculo = vehiculoRepository.findById(solicitud.vehiculoId!!)
            .orElseThrow { NoEncontradoException("El vehículo no existe") }

        if (!conductor.disponible) throw ReglaNegocioException("El conductor no está disponible")
        val entrada = programacionService.deHoy(conductor.id!!)?.entrada(ruta.id!!)
        if (entrada == null || entrada.retiroPendiente) {
            throw ReglaNegocioException("La ruta no está en la programación de hoy del conductor")
        }
        if (!vehiculo.activo) throw ReglaNegocioException("El vehículo está inactivo y no puede usarse en recorridos") // RN-25
        if (recorridoActivo.conductorTieneActivo(conductor.id!!)) {
            throw ReglaNegocioException("El conductor ya tiene un recorrido activo")
        }
        if (recorridoActivo.vehiculoTieneActivo(vehiculo.id!!)) {
            throw ReglaNegocioException("El vehículo ya está en otro recorrido activo")
        }

        val recorrido = recorridoRepository.saveAndFlush(
            Recorrido(conductor = conductor, ruta = ruta, vehiculo = vehiculo, estado = EstadoRecorrido.ACTIVO, inicio = inicio),
        )
        return responder(recorrido)
    }

    /** Finaliza el recorrido y aplica el retiro pendiente de su ruta, si lo había (D-11). */
    @Transactional
    fun finalizar(id: Long): RecorridoDemoResponse {
        val recorrido = recorridoRepository.findById(id).orElseThrow { NoEncontradoException("El recorrido no existe") }
        if (recorrido.estado == EstadoRecorrido.FINALIZADO) throw ReglaNegocioException("El recorrido ya está finalizado")

        recorrido.estado = EstadoRecorrido.FINALIZADO
        recorrido.fin = clock.instant()
        recorridoRepository.saveAndFlush(recorrido)
        programacionService.aplicarRetiroPendiente(recorrido.conductor.id!!, recorrido.ruta.id!!)
        return responder(recorrido)
    }

    private fun responder(recorrido: Recorrido) = RecorridoDemoResponse(
        id = recorrido.id!!,
        conductor = ConductorRecorridoResponse(recorrido.conductor.id!!, recorrido.conductor.nombre),
        ruta = RutaRecorridoResponse(recorrido.ruta.id!!, recorrido.ruta.codigo, recorrido.ruta.nombre),
        vehiculo = VehiculoRecorridoResponse(recorrido.vehiculo.id!!, recorrido.vehiculo.numeroInterno, recorrido.vehiculo.placa),
        estado = recorrido.estado,
        inicio = recorrido.inicio.atZone(clock.zone).toOffsetDateTime(),
        fin = recorrido.fin?.atZone(clock.zone)?.toOffsetDateTime(),
    )
}
