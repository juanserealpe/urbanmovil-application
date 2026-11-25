package co.urbanmovil.api.ruta

import co.urbanmovil.api.common.CampoError
import co.urbanmovil.api.common.DatoRepetidoException
import co.urbanmovil.api.common.NoEncontradoException
import co.urbanmovil.api.common.ReglaNegocioException
import co.urbanmovil.api.common.Texto
import co.urbanmovil.api.common.ValidacionException
import co.urbanmovil.api.programacion.ProgramacionConsulta
import co.urbanmovil.api.recorrido.RecorridoActivoConsulta
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class RutaService(
    private val rutaRepository: RutaRepository,
    private val recorridoActivo: RecorridoActivoConsulta,
    private val programacionConsulta: ProgramacionConsulta,
    private val clock: Clock,
) {

    /** HU-04, HU-05: lista resumida. `q` busca en código o nombre. */
    @Transactional(readOnly = true)
    fun listar(q: String?): List<RutaResumenResponse> {
        val busqueda = Texto.vacioANulo(q)
        val activos = recorridoActivo.cantidadPorRuta()
        val conductoresHoy = programacionConsulta.conductoresPorRuta()
        return rutaRepository.findAllBy(ORDEN)
            .filter { busqueda == null || Texto.contiene(it.codigo, busqueda) || Texto.contiene(it.nombre, busqueda) }
            .map { resumen(it, activos[it.id] ?: 0, conductoresHoy[it.id].orEmpty()) }
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): RutaDetalleResponse = detalle(buscar(id))

    /** Endpoint público: siempre lee la BD, así refleja los cambios al instante. */
    @Transactional(readOnly = true)
    fun listarPublicas(): List<RutaPublicaResponse> =
        rutaRepository.findAllBy(ORDEN).map { ruta ->
            RutaPublicaResponse(
                codigo = ruta.codigo,
                nombre = ruta.nombre,
                color = ruta.color,
                recorridoPrincipal = ruta.recorridoPrincipal,
                alternos = ruta.alternos.map { CaminoAlternoPublicoResponse(it.nombre, it.puntos) },
            )
        }

    /** HU-02, HU-03 · CA-02.1 a CA-02.5, CA-03.1. */
    @Transactional
    fun registrar(solicitud: RutaRequest): RutaDetalleResponse {
        val datos = validar(solicitud, idActual = null)
        val ahora = clock.instant()
        val ruta = Ruta(
            codigo = datos.codigo,
            nombre = datos.nombre,
            color = datos.color ?: siguienteColorDePaleta(),
            recorridoPrincipal = datos.recorridoPrincipal,
            creadoEn = ahora,
            actualizadoEn = ahora,
        )
        datos.alternos.forEach { ruta.alternos.add(CaminoAlterno(ruta, it.nombre, it.puntos)) }
        return detalle(rutaRepository.save(ruta))
    }

    /**
     * HU-03, HU-04 · CA-04.1 a CA-04.3. Reemplaza todo: un alterno con `id` se actualiza,
     * uno sin `id` se crea y uno que no viene se elimina. Si algo es inválido no cambia nada.
     */
    @Transactional
    fun modificar(id: Long, solicitud: RutaRequest): RutaDetalleResponse {
        val ruta = buscar(id)
        val datos = validar(solicitud, idActual = id)

        val existentes = ruta.alternos.associateBy { it.id }
        val idsRecibidos = datos.alternos.mapNotNull { it.id }
        datos.alternos.forEachIndexed { i, alterno ->
            if (alterno.id != null && alterno.id !in existentes) {
                throw ValidacionException("alternos[$i].id", "El camino alterno no pertenece a esta ruta")
            }
        }
        val idsConservados: Set<Long?> = idsRecibidos.toSet()
        if (idsRecibidos.size != idsConservados.size) {
            throw ValidacionException("alternos", "Un camino alterno viene repetido")
        }

        ruta.codigo = datos.codigo
        ruta.nombre = datos.nombre
        ruta.color = datos.color ?: ruta.color
        ruta.recorridoPrincipal = datos.recorridoPrincipal
        ruta.actualizadoEn = clock.instant()

        ruta.alternos.removeIf { it.id !in idsConservados } // CA-04.3 (orphanRemoval los borra)
        datos.alternos.forEach { alterno ->
            val existente = alterno.id?.let { existentes.getValue(it) }
            if (existente != null) {
                existente.nombre = alterno.nombre
                existente.puntos = alterno.puntos
            } else {
                ruta.alternos.add(CaminoAlterno(ruta, alterno.nombre, alterno.puntos))
            }
        }
        return detalle(rutaRepository.saveAndFlush(ruta))
    }

    /**
     * HU-05 · CA-05.1 / CA-05.2. La BD borra en cascada alternos, `programacion_ruta`,
     * `reasignacion_ruta` y recorridos finalizados (D-17).
     */
    @Transactional
    fun eliminar(id: Long) {
        val ruta = buscar(id)
        if (recorridoActivo.rutaTieneActivos(id)) {
            throw ReglaNegocioException("No se puede eliminar: la ruta tiene recorridos activos.")
        }
        rutaRepository.delete(ruta)
    }

    // ------------------------------------------------------------------ Apoyo

    private data class AlternoValidado(val id: Long?, val nombre: String, val puntos: List<Punto>)

    private data class RutaValidada(
        val codigo: String,
        val nombre: String,
        val color: String?,
        val recorridoPrincipal: List<Punto>,
        val alternos: List<AlternoValidado>,
    )

    private fun validar(solicitud: RutaRequest, idActual: Long?): RutaValidada {
        val recorrido = TrazadoValidador.recorridoPrincipal(solicitud.recorridoPrincipal)
        val alternos = TrazadoValidador.alternos(solicitud.alternos).mapIndexed { i, alterno ->
            AlternoValidado(alterno.id, alterno.nombre!!.trim(), TrazadoValidador.puntosAlterno(i, alterno.puntos))
        }
        val codigo = Texto.mayusculas(solicitud.codigo!!)
        val nombre = solicitud.nombre!!.trim()
        validarUnicidad(codigo, nombre, idActual)
        return RutaValidada(
            codigo = codigo,
            nombre = nombre,
            color = Texto.vacioANulo(solicitud.color)?.uppercase(),
            recorridoPrincipal = recorrido,
            alternos = alternos,
        )
    }

    private fun validarUnicidad(codigo: String, nombre: String, idActual: Long?) {
        val codigoRepetido = if (idActual == null) {
            rutaRepository.existsByCodigoIgnoreCase(codigo)
        } else {
            rutaRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, idActual)
        }
        val nombreRepetido = if (idActual == null) {
            rutaRepository.existsByNombreIgnoreCase(nombre)
        } else {
            rutaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idActual)
        }
        val campos = buildList {
            if (codigoRepetido) add(CampoError("codigo", "Ya existe una ruta con este código"))
            if (nombreRepetido) add(CampoError("nombre", "Ya existe una ruta con este nombre"))
        }
        if (campos.isNotEmpty()) throw DatoRepetidoException(campos)
    }

    /** Color de la paleta fija, en orden de creación. */
    private fun siguienteColorDePaleta(): String = PALETA[(rutaRepository.count() % PALETA.size).toInt()]

    private fun buscar(id: Long): Ruta =
        rutaRepository.findById(id).orElseThrow { NoEncontradoException("La ruta no existe") }

    private fun resumen(ruta: Ruta, recorridosActivos: Long, conductoresHoy: List<String>) = RutaResumenResponse(
        id = ruta.id!!,
        codigo = ruta.codigo,
        nombre = ruta.nombre,
        color = ruta.color,
        puntosRecorrido = ruta.recorridoPrincipal.size,
        numAlternos = ruta.alternos.size,
        recorridosActivos = recorridosActivos,
        conductoresHoy = conductoresHoy,
        actualizadoEn = ruta.actualizadoEn.atZone(clock.zone).toOffsetDateTime(),
    )

    private fun detalle(ruta: Ruta): RutaDetalleResponse {
        val r = resumen(
            ruta,
            recorridoActivo.cantidadPorRuta()[ruta.id] ?: 0,
            programacionConsulta.conductoresPorRuta()[ruta.id].orEmpty(),
        )
        return RutaDetalleResponse(
            id = r.id,
            codigo = r.codigo,
            nombre = r.nombre,
            color = r.color,
            puntosRecorrido = r.puntosRecorrido,
            numAlternos = r.numAlternos,
            recorridosActivos = r.recorridosActivos,
            conductoresHoy = r.conductoresHoy,
            actualizadoEn = r.actualizadoEn,
            recorridoPrincipal = ruta.recorridoPrincipal,
            inicio = ruta.inicio,
            final = ruta.final,
            alternos = ruta.alternos.map { CaminoAlternoResponse(it.id!!, it.nombre, it.puntos) },
        )
    }

    companion object {
        private val ORDEN = Sort.by("codigo")

        /** Paleta fija de 6 colores (se asigna en orden de creación si no llega `color`). */
        val PALETA = listOf("#0057B8", "#D62828", "#2A9D8F", "#F4A261", "#6A4C93", "#E76F51")
    }
}
