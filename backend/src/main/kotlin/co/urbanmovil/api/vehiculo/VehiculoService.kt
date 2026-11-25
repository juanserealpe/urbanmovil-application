package co.urbanmovil.api.vehiculo

import co.urbanmovil.api.common.CampoError
import co.urbanmovil.api.common.DatoRepetidoException
import co.urbanmovil.api.common.NoEncontradoException
import co.urbanmovil.api.common.ReglaNegocioException
import co.urbanmovil.api.common.Texto
import co.urbanmovil.api.common.ValidacionException
import co.urbanmovil.api.recorrido.EnRecorridoResponse
import co.urbanmovil.api.recorrido.RecorridoActivoConsulta
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class VehiculoService(
    private val vehiculoRepository: VehiculoRepository,
    private val recorridoActivo: RecorridoActivoConsulta,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun listar(q: String?, activo: Boolean?): List<VehiculoResponse> {
        val busqueda = Texto.vacioANulo(q)
        val enRecorrido = recorridoActivo.porVehiculo()
        return vehiculoRepository.findAll(Sort.by("numeroInterno"))
            .asSequence()
            .filter { activo == null || it.activo == activo }
            .filter {
                busqueda == null ||
                    Texto.contiene(it.placa, busqueda) ||
                    Texto.contiene(it.placa, Texto.normalizarPlaca(busqueda)) ||
                    Texto.contiene(it.numeroInterno, busqueda)
            }
            .map { responder(it, enRecorrido[it.id]) }
            .toList()
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): VehiculoResponse = responder(buscar(id))

    /** HU-28 · CA-28.1 / CA-28.2. */
    @Transactional
    fun registrar(solicitud: VehiculoRequest): VehiculoResponse {
        val (placa, numeroInterno) = normalizar(solicitud)
        validarUnicidad(placa, numeroInterno, idActual = null)

        val ahora = clock.instant()
        val vehiculo = vehiculoRepository.save(
            Vehiculo(placa = placa, numeroInterno = numeroInterno, creadoEn = ahora, actualizadoEn = ahora),
        )
        return responder(vehiculo, enRecorrido = null)
    }

    /** HU-29 · CA-29.1. */
    @Transactional
    fun modificar(id: Long, solicitud: VehiculoRequest): VehiculoResponse {
        val vehiculo = buscar(id)
        val (placa, numeroInterno) = normalizar(solicitud)
        validarUnicidad(placa, numeroInterno, idActual = id)

        vehiculo.placa = placa
        vehiculo.numeroInterno = numeroInterno
        vehiculo.actualizadoEn = clock.instant()
        return responder(vehiculo)
    }

    /** HU-29 · CA-29.2 / CA-29.3 (RN-26). D-17: la BD borra en cascada su historial. */
    @Transactional
    fun eliminar(id: Long) {
        val vehiculo = buscar(id)
        if (recorridoActivo.vehiculoTieneActivo(id)) {
            throw ReglaNegocioException(
                "No se puede eliminar: el vehículo tiene un recorrido activo. Debe finalizarse primero.",
            )
        }
        vehiculoRepository.delete(vehiculo)
    }

    /** HU-30 · CA-30.1 a CA-30.4. */
    @Transactional
    fun cambiarEstado(id: Long, solicitud: EstadoVehiculoRequest): VehiculoResponse {
        val vehiculo = buscar(id)
        val ahora = clock.instant()

        if (solicitud.activo!!) {
            vehiculo.reactivar(ahora)
            return responder(vehiculo)
        }

        val motivo = Texto.vacioANulo(solicitud.motivo)
            ?: throw ValidacionException("motivo", "Escriba el motivo de la desactivación")
        if (recorridoActivo.vehiculoTieneActivo(id)) {
            throw ReglaNegocioException("No se puede desactivar mientras tiene un recorrido activo.") // RN-26
        }
        vehiculo.desactivar(motivo, ahora)
        return responder(vehiculo)
    }

    private fun normalizar(solicitud: VehiculoRequest): Pair<String, String> {
        val placa = Texto.normalizarPlaca(solicitud.placa!!)
        if (placa.isEmpty()) throw ValidacionException("placa", "Escriba la placa")
        return placa to Texto.mayusculas(solicitud.numeroInterno!!)
    }

    private fun validarUnicidad(placa: String, numeroInterno: String, idActual: Long?) {
        val placaRepetida = if (idActual == null) {
            vehiculoRepository.existsByPlacaIgnoreCase(placa)
        } else {
            vehiculoRepository.existsByPlacaIgnoreCaseAndIdNot(placa, idActual)
        }
        val numeroRepetido = if (idActual == null) {
            vehiculoRepository.existsByNumeroInternoIgnoreCase(numeroInterno)
        } else {
            vehiculoRepository.existsByNumeroInternoIgnoreCaseAndIdNot(numeroInterno, idActual)
        }

        val campos = buildList {
            if (placaRepetida) add(CampoError("placa", "Ya existe un vehículo con esta placa"))
            if (numeroRepetido) add(CampoError("numeroInterno", "Ya existe un vehículo con este número interno"))
        }
        if (campos.isNotEmpty()) throw DatoRepetidoException(campos)
    }

    private fun buscar(id: Long): Vehiculo =
        vehiculoRepository.findById(id).orElseThrow { NoEncontradoException("El vehículo no existe") }

    private fun responder(vehiculo: Vehiculo) = responder(vehiculo, recorridoActivo.porVehiculo()[vehiculo.id])

    private fun responder(vehiculo: Vehiculo, enRecorrido: EnRecorridoResponse?) = VehiculoResponse(
        id = vehiculo.id!!,
        placa = vehiculo.placa,
        numeroInterno = vehiculo.numeroInterno,
        activo = vehiculo.activo,
        motivoInactivacion = vehiculo.motivoInactivacion,
        fechaInactivacion = vehiculo.fechaInactivacion?.atZone(clock.zone)?.toOffsetDateTime(),
        enRecorrido = enRecorrido,
    )
}
