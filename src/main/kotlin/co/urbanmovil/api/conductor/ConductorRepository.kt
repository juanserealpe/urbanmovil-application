package co.urbanmovil.api.conductor

import org.springframework.data.jpa.repository.JpaRepository

interface ConductorRepository : JpaRepository<Conductor, Long> {

    fun existsByIdentificacionIgnoreCase(identificacion: String): Boolean

    fun existsByIdentificacionIgnoreCaseAndIdNot(identificacion: String, id: Long): Boolean

    fun existsByCorreoIgnoreCase(correo: String): Boolean

    fun existsByCorreoIgnoreCaseAndIdNot(correo: String, id: Long): Boolean
}
