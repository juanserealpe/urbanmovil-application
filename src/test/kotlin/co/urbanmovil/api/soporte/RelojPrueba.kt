package co.urbanmovil.api.soporte

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/** Clock de prueba: hora fija que las pruebas pueden mover (`avanzar`, `fijar`). */
class RelojPrueba(
    @Volatile private var actual: Instant = INICIO,
    private val zona: ZoneId = ZONA,
) : Clock() {

    override fun getZone(): ZoneId = zona

    override fun withZone(zone: ZoneId): Clock = Clock.fixed(actual, zone)

    override fun instant(): Instant = actual

    fun fijar(instante: Instant) {
        actual = instante
    }

    fun avanzar(duracion: Duration) {
        actual = actual.plus(duracion)
    }

    fun reiniciar() = fijar(INICIO)

    companion object {
        val ZONA: ZoneId = ZoneId.of("America/Bogota")

        /** «Hoy» en todas las pruebas: 24 de noviembre de 2026, 10:00 en Bogotá. */
        val INICIO: Instant = OffsetDateTime.parse("2026-11-24T10:00:00-05:00").toInstant()
    }
}

/** Reemplaza el Clock real en las pruebas (`@Primary` gana sobre el bean de ClockConfig). */
@TestConfiguration(proxyBeanMethods = false)
class RelojPruebaConfig {

    @Bean
    @Primary
    fun relojPrueba(): RelojPrueba = RelojPrueba()
}
