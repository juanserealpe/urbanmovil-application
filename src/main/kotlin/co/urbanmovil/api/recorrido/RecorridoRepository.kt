package co.urbanmovil.api.recorrido

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/** Fila liviana de un recorrido: evita cargar conductor, vehículo y ruta completos. */
data class RecorridoFila(
    val recorridoId: Long,
    val conductorId: Long,
    val vehiculoId: Long,
    val rutaId: Long,
    val rutaCodigo: String,
)

/** Cantidad de recorridos de una ruta en un estado. */
data class ConteoPorRuta(
    val rutaId: Long,
    val cantidad: Long,
)

interface RecorridoRepository : JpaRepository<Recorrido, Long> {

    fun existsByConductorIdAndEstado(conductorId: Long, estado: EstadoRecorrido): Boolean

    fun existsByVehiculoIdAndEstado(vehiculoId: Long, estado: EstadoRecorrido): Boolean

    fun existsByRutaIdAndEstado(rutaId: Long, estado: EstadoRecorrido): Boolean

    @Query(
        """
        select new co.urbanmovil.api.recorrido.ConteoPorRuta(r.ruta.id, count(r))
        from Recorrido r
        where r.estado = :estado
        group by r.ruta.id
        """,
    )
    fun contarPorRuta(estado: EstadoRecorrido): List<ConteoPorRuta>

    @Query(
        """
        select new co.urbanmovil.api.recorrido.RecorridoFila(r.id, r.conductor.id, r.vehiculo.id, r.ruta.id, r.ruta.codigo)
        from Recorrido r
        where r.estado = :estado
        """,
    )
    fun findFilasPorEstado(estado: EstadoRecorrido): List<RecorridoFila>

    /** Recorridos con conductor, ruta y vehículo ya cargados (monitoreo y lista de demo). */
    @Query(
        """
        select r from Recorrido r
        join fetch r.conductor join fetch r.ruta join fetch r.vehiculo
        order by r.inicio desc, r.id desc
        """,
    )
    fun findTodosCompletos(): List<Recorrido>

    @Query(
        """
        select r from Recorrido r
        join fetch r.conductor join fetch r.ruta join fetch r.vehiculo
        where r.estado = :estado
        order by r.inicio desc, r.id desc
        """,
    )
    fun findCompletosPorEstado(estado: EstadoRecorrido): List<Recorrido>
}
