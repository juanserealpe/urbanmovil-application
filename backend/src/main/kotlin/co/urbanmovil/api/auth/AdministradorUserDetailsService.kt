package co.urbanmovil.api.auth

import co.urbanmovil.api.administrador.AdministradorRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Solo busca en la tabla `administrador`: un conductor nunca puede iniciar sesión
 * en la web aunque exista su correo (CA-01.3).
 */
@Service
class AdministradorUserDetailsService(
    private val administradorRepository: AdministradorRepository,
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val administrador = administradorRepository.findByCorreoIgnoreCase(username.trim())
            ?: throw UsernameNotFoundException("Credenciales inválidas")
        return User.withUsername(administrador.correo)
            .password(administrador.hashContrasena)
            .roles(ROL_ADMIN)
            .build()
    }

    companion object {
        const val ROL_ADMIN = "ADMIN"
    }
}
