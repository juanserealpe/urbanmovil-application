package co.urbanmovil.api.ruta

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface RutaRepository : JpaRepository<Ruta, Long> {

    /** Trae las rutas con sus alternos en una sola consulta. */
    @EntityGraph(attributePaths = ["alternos"])
    fun findAllBy(sort: Sort): List<Ruta>

    fun existsByCodigoIgnoreCase(codigo: String): Boolean

    fun existsByCodigoIgnoreCaseAndIdNot(codigo: String, id: Long): Boolean

    fun existsByNombreIgnoreCase(nombre: String): Boolean

    fun existsByNombreIgnoreCaseAndIdNot(nombre: String, id: Long): Boolean
}
