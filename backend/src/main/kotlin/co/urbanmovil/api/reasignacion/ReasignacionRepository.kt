package co.urbanmovil.api.reasignacion

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ReasignacionRepository : JpaRepository<Reasignacion, Long> {

    /** Historial completo, más reciente primero, con todo lo que muestra la respuesta. */
    @EntityGraph(attributePaths = ["conductor", "administrador", "rutas", "rutas.ruta"])
    fun findAllByOrderByFechaHoraDescIdDesc(): List<Reasignacion>
}
