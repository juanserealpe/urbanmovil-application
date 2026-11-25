package co.urbanmovil.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger usa la misma cookie de sesión que el panel: tras llamar a
 * POST /api/v1/auth/login desde Swagger, el navegador guarda JSESSIONID
 * y los demás endpoints funcionan sin configurar nada más.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("UrbanMovil · API")
                .version("v1")
                .description(
                    "Backend de UrbanMovil: panel de administración y endpoints públicos para las apps. " +
                        "Inicie sesión con POST /api/v1/auth/login para probar los endpoints protegidos.",
                ),
        )
        .components(
            Components().addSecuritySchemes(
                ESQUEMA_SESION,
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name("JSESSIONID")
                    .description("Cookie de sesión obtenida con POST /api/v1/auth/login"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList(ESQUEMA_SESION))

    companion object {
        const val ESQUEMA_SESION = "sesion"
    }
}
