package co.urbanmovil.api.programacion

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ProgramacionRepository : JpaRepository<Programacion, Long> {

    @EntityGraph(attributePaths = ["rutas", "rutas.ruta"])
    fun findByConductorIdAndFecha(conductorId: Long, fecha: LocalDate): Programacion?
}

/** Fila liviana de la programación de un día. */
data class ProgramacionFila(
    val conductorId: Long,
    val conductorNombre: String,
    val rutaId: Long,
    val rutaCodigo: String,
    val orden: Int,
)

interface ProgramacionRutaRepository : JpaRepository<ProgramacionRuta, Long> {

    @Query(
        """
        select new co.urbanmovil.api.programacion.ProgramacionFila(c.id, c.nombre, r.id, r.codigo, pr.orden)
        from ProgramacionRuta pr
        join pr.programacion p
        join p.conductor c
        join pr.ruta r
        where p.fecha = :fecha
        order by pr.orden
        """,
    )
    fun filasDelDia(fecha: LocalDate): List<ProgramacionFila>
}
