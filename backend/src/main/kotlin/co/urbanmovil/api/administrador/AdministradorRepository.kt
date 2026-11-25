package co.urbanmovil.api.administrador

import org.springframework.data.jpa.repository.JpaRepository

interface AdministradorRepository : JpaRepository<Administrador, Long> {
    fun findByCorreoIgnoreCase(correo: String): Administrador?
}
