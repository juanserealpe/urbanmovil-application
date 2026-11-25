package co.urbanmovil.api.auth

import co.urbanmovil.api.administrador.Administrador
import co.urbanmovil.api.administrador.AdministradorRepository
import co.urbanmovil.api.common.Texto
import co.urbanmovil.api.config.UrbanMovilProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** D-04: si no hay administradores, se crea uno con los datos de `urbanmovil.admin-inicial`. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdministradorInicialRunner(
    private val administradorRepository: AdministradorRepository,
    private val passwordEncoder: PasswordEncoder,
    private val propiedades: UrbanMovilProperties,
    private val clock: Clock,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (administradorRepository.count() > 0) return

        val datos = propiedades.adminInicial
        administradorRepository.save(
            Administrador(
                nombre = datos.nombre.trim(),
                correo = Texto.normalizarCorreo(datos.correo),
                hashContrasena = passwordEncoder.encode(datos.contrasena)!!,
                creadoEn = clock.instant(),
            ),
        )
        log.info("Administrador inicial creado con el correo {}", Texto.normalizarCorreo(datos.correo))
    }
}
