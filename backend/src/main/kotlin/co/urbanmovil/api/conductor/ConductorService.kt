package co.urbanmovil.api.conductor

import co.urbanmovil.api.common.CampoError
import co.urbanmovil.api.common.DatoRepetidoException
import co.urbanmovil.api.common.NoEncontradoException
import co.urbanmovil.api.common.ReglaNegocioException
import co.urbanmovil.api.common.Texto
import co.urbanmovil.api.common.ValidacionException
import co.urbanmovil.api.programacion.ProgramacionConsulta
import co.urbanmovil.api.recorrido.RecorridoActivoConsulta
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class ConductorService(
    private val conductorRepository: ConductorRepository,
    private val recorridoActivo: RecorridoActivoConsulta,
    private val programacionConsulta: ProgramacionConsulta,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun listar(q: String?, disponible: Boolean?): List<ConductorResponse> {
        val busqueda = Texto.vacioANulo(q)
        val enRecorrido = recorridoActivo.porConductor()
        val rutasHoy = programacionConsulta.rutasPorConductor()
        return conductorRepository.findAll(Sort.by("nombre"))
            .asSequence()
            .filter { disponible == null || it.disponible == disponible }
            .filter {
                busqueda == null ||
                    Texto.contiene(it.nombre, busqueda) ||
                    Texto.contiene(it.identificacion, busqueda) ||
                    Texto.contiene(it.correo, busqueda)
            }
            .map { ConductorResponse.de(it, enRecorrido[it.id], rutasHoy[it.id].orEmpty()) }
            .toList()
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): ConductorResponse = responder(buscar(id))

    /** HU-06 · CA-06.1 / CA-06.2. */
    @Transactional
    fun registrar(solicitud: ConductorCrearRequest): ConductorResponse {
        val identificacion = solicitud.identificacion!!.trim()
        val correo = Texto.normalizarCorreo(solicitud.correo!!)
        validarUnicidad(identificacion, correo, idActual = null)

        val ahora = clock.instant()
        val conductor = conductorRepository.save(
            Conductor(
                nombre = solicitud.nombre!!.trim(),
                identificacion = identificacion,
                correo = correo,
                telefono = solicitud.telefono!!.trim(),
                hashContrasena = cifrar(solicitud.contrasena!!),
                creadoEn = ahora,
                actualizadoEn = ahora,
            ),
        )
        return ConductorResponse.de(conductor, enRecorrido = null)
    }

    /** HU-07 · CA-07.1. No toca la disponibilidad. */
    @Transactional
    fun modificar(id: Long, solicitud: ConductorModificarRequest): ConductorResponse {
        val conductor = buscar(id)
        val identificacion = solicitud.identificacion!!.trim()
        val correo = Texto.normalizarCorreo(solicitud.correo!!)
        validarUnicidad(identificacion, correo, idActual = id)

        conductor.nombre = solicitud.nombre!!.trim()
        conductor.identificacion = identificacion
        conductor.correo = correo
        conductor.telefono = solicitud.telefono!!.trim()
        Texto.vacioANulo(solicitud.contrasena)?.let { conductor.hashContrasena = cifrar(it) }
        conductor.actualizadoEn = clock.instant()
        return responder(conductor)
    }

    /** HU-07 · CA-07.2 / CA-07.3 (D-05). D-17: la BD borra en cascada su historial. */
    @Transactional
    fun eliminar(id: Long) {
        val conductor = buscar(id)
        if (recorridoActivo.conductorTieneActivo(id)) {
            throw ReglaNegocioException(
                "No se puede eliminar: el conductor tiene un recorrido activo. Debe finalizarlo primero.",
            )
        }
        conductorRepository.delete(conductor)
    }

    /** HU-08 · CA-08.1 a CA-08.4 y V-6. */
    @Transactional
    fun cambiarDisponibilidad(id: Long, solicitud: DisponibilidadRequest): ConductorResponse {
        val conductor = buscar(id)
        val ahora = clock.instant()

        if (solicitud.disponible!!) {
            conductor.marcarDisponible(ahora)
            return responder(conductor)
        }

        val motivo = solicitud.motivo ?: throw ValidacionException("motivo", "Seleccione el motivo")
        val detalle = Texto.vacioANulo(solicitud.detalle)
        if (motivo == MotivoNoDisponible.OTRO && detalle == null) {
            throw ValidacionException("detalle", "Describa el motivo") // V-6 (provisional)
        }
        if (recorridoActivo.conductorTieneActivo(id)) {
            throw ReglaNegocioException("No se puede marcar como no disponible mientras tiene un recorrido activo.") // D-08
        }
        conductor.marcarNoDisponible(motivo, detalle, ahora)
        return responder(conductor)
    }

    private fun validarUnicidad(identificacion: String, correo: String, idActual: Long?) {
        val identificacionRepetida = if (idActual == null) {
            conductorRepository.existsByIdentificacionIgnoreCase(identificacion)
        } else {
            conductorRepository.existsByIdentificacionIgnoreCaseAndIdNot(identificacion, idActual)
        }
        val correoRepetido = if (idActual == null) {
            conductorRepository.existsByCorreoIgnoreCase(correo)
        } else {
            conductorRepository.existsByCorreoIgnoreCaseAndIdNot(correo, idActual)
        }

        val campos = buildList {
            if (identificacionRepetida) {
                add(CampoError("identificacion", "Ya existe un conductor con este número de identificación"))
            }
            if (correoRepetido) add(CampoError("correo", "Ya existe un conductor con este correo"))
        }
        if (campos.isNotEmpty()) throw DatoRepetidoException(campos)
    }

    private fun buscar(id: Long): Conductor =
        conductorRepository.findById(id).orElseThrow { NoEncontradoException("El conductor no existe") }

    private fun responder(conductor: Conductor) =
        ConductorResponse.de(
            conductor,
            recorridoActivo.porConductor()[conductor.id],
            programacionConsulta.rutasPorConductor()[conductor.id].orEmpty(),
        )

    private fun cifrar(contrasena: String): String = passwordEncoder.encode(contrasena)!!
}
