package co.urbanmovil.api.soporte

import co.urbanmovil.api.TestcontainersConfiguration
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.json.JsonMapper

/**
 * Base de las pruebas de integración: contexto completo + PostgreSQL real (Testcontainers)
 * + MockMvc con la cadena de filtros de Spring Security.
 * Todas las subclases comparten el mismo contexto (y por lo tanto el mismo contenedor).
 * La hora la fija [RelojPrueba]; el perfil demo está activo, así que la semilla corre al
 * arrancar y cada prueba empieza con la BD vacía (salvo el administrador inicial).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, RelojPruebaConfig::class)
@TestPropertySource(properties = ["urbanmovil.demo.habilitado=true"]) // endpoints de demo (D-16) disponibles
abstract class IntegracionTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var jdbc: JdbcTemplate

    @Autowired
    protected lateinit var jsonMapper: JsonMapper

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var reloj: RelojPrueba

    /** Deja la BD limpia antes de cada prueba (el administrador inicial se conserva). */
    @BeforeEach
    fun limpiarBaseDeDatos() {
        reloj.reiniciar()
        jdbc.execute(
            """
            TRUNCATE conductor, vehiculo, ruta, camino_alterno, programacion, programacion_ruta,
                     reasignacion, reasignacion_ruta, recorrido
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }

    protected fun json(vararg pares: Pair<String, Any?>): String = jsonMapper.writeValueAsString(mapOf(*pares))

    protected fun insertarConductor(
        identificacion: String = "1061999999",
        correo: String = "conductor.bd@prueba.co",
        contrasena: String = "Conductor123",
        nombre: String = "Conductor de Prueba",
    ): Long = jdbc.queryForObject(
        """
        INSERT INTO conductor (nombre, identificacion, correo, telefono, hash_contrasena)
        VALUES (?, ?, ?, '3000000000', ?) RETURNING id
        """.trimIndent(),
        Long::class.javaObjectType,
        nombre, identificacion, correo, passwordEncoder.encode(contrasena),
    )!!

    protected fun insertarVehiculo(placa: String = "ZZZ999", numeroInterno: String = "UM-999"): Long =
        jdbc.queryForObject(
            "INSERT INTO vehiculo (placa, numero_interno) VALUES (?, ?) RETURNING id",
            Long::class.javaObjectType,
            placa, numeroInterno,
        )!!

    protected fun insertarRuta(codigo: String = "R1"): Long = jdbc.queryForObject(
        "INSERT INTO ruta (codigo, nombre, color) VALUES (?, ?, '#0057B8') RETURNING id",
        Long::class.javaObjectType,
        codigo, "Ruta $codigo",
    )!!

    /**
     * En la Fase 1 no hay endpoint para crear recorridos: se insertan directo en la BD.
     * Crea lo que falte (conductor, vehículo, ruta) y devuelve el id del recorrido.
     */
    protected fun insertarRecorrido(
        conductorId: Long? = null,
        vehiculoId: Long? = null,
        estado: String = "ACTIVO",
        rutaCodigo: String = "R1",
    ): Long {
        val conductor = conductorId ?: insertarConductor(identificacion = "REC-${System.nanoTime() % 1_000_000}", correo = "rec${System.nanoTime()}@prueba.co")
        val vehiculo = vehiculoId ?: insertarVehiculo(placa = "R${System.nanoTime() % 100_000}", numeroInterno = "REC-${System.nanoTime() % 100_000}")
        val ruta = jdbc.queryForList("SELECT id FROM ruta WHERE codigo = ?", Long::class.javaObjectType, rutaCodigo).firstOrNull()
            ?: insertarRuta(rutaCodigo)
        return jdbc.queryForObject(
            """
            INSERT INTO recorrido (conductor_id, ruta_id, vehiculo_id, estado, inicio, fin)
            VALUES (?, ?, ?, ?, now(), CASE WHEN ? = 'FINALIZADO' THEN now() END) RETURNING id
            """.trimIndent(),
            Long::class.javaObjectType,
            conductor, ruta, vehiculo, estado, estado,
        )!!
    }
}
