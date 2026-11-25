package co.urbanmovil.api.config

import co.urbanmovil.api.common.JsonAccessDeniedHandler
import co.urbanmovil.api.common.JsonAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository

/**
 * Seguridad del MVP (HU-01):
 * - Sesión de servidor (cookie JSESSIONID HttpOnly, SameSite=Lax). Sin JWT.
 * - CSRF deshabilitado para `/api/` y todo lo que cuelga de ahí (simplificación documentada en el README).
 * - Solo los administradores pueden autenticarse (ver AdministradorUserDetailsService).
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        entryPoint: JsonAuthenticationEntryPoint,
        accessDeniedHandler: JsonAccessDeniedHandler,
        securityContextRepository: SecurityContextRepository,
    ): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())
            .csrf { it.ignoringRequestMatchers("/api/**") }
            .securityContext { it.securityContextRepository(securityContextRepository) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/api/v1/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() } // el logout lo expone AuthController como endpoint JSON
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityContextRepository(): SecurityContextRepository = HttpSessionSecurityContextRepository()

    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder,
    ): AuthenticationManager {
        val proveedor = DaoAuthenticationProvider(userDetailsService).apply { setPasswordEncoder(passwordEncoder) }
        return ProviderManager(proveedor)
    }
}
