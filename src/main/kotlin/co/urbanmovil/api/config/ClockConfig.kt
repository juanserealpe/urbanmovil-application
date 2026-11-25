package co.urbanmovil.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

/**
 * «Hoy» y «ahora» siempre se calculan con este Clock (zona America/Bogota),
 * así las pruebas pueden fijar la hora reemplazando el bean.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    fun clock(propiedades: UrbanMovilProperties): Clock =
        Clock.system(ZoneId.of(propiedades.zonaHoraria))
}
