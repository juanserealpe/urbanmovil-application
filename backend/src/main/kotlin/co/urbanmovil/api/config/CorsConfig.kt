package co.urbanmovil.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration(proxyBeanMethods = false)
class CorsConfig {

    @Bean
    fun corsConfigurationSource(propiedades: UrbanMovilProperties): CorsConfigurationSource {
        val configuracion = CorsConfiguration().apply {
            allowedOrigins = propiedades.cors.origenes.map { it.trim() }.filter { it.isNotEmpty() }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuracion)
        }
    }
}
