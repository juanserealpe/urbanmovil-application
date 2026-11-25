package co.urbanmovil.api.auth

import co.urbanmovil.api.soporte.IntegracionTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** HU-01 · CA-01.1, CA-01.2, CA-01.3 + seguridad general. */
class AuthIntegracionTest : IntegracionTest() {

    private val correoAdmin = "admin@urbanmovil.co"
    private val contrasenaAdmin = "Admin12345"

    private fun login(correo: String, contrasena: String) = mockMvc.post("/api/v1/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = json("correo" to correo, "contrasena" to contrasena)
    }

    private fun iniciarSesionAdmin(): MockHttpSession {
        val resultado = login(correoAdmin, contrasenaAdmin).andExpect { status { isOk() } }.andReturn()
        return assertNotNull(resultado.request.getSession(false) as MockHttpSession?)
    }

    @Test
    fun `CA-01_1 login valido devuelve el administrador y permite usar endpoints protegidos`() {
        login(correoAdmin, contrasenaAdmin).andExpect {
            status { isOk() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.nombre") { value("Administrador") }
            jsonPath("$.correo") { value(correoAdmin) }
            jsonPath("$.contrasena") { doesNotExist() }
            jsonPath("$.hashContrasena") { doesNotExist() }
        }

        val sesion = iniciarSesionAdmin()
        mockMvc.get("/api/v1/conductores") { session = sesion }.andExpect { status { isOk() } }
        mockMvc.get("/api/v1/auth/me") { session = sesion }.andExpect {
            status { isOk() }
            jsonPath("$.correo") { value(correoAdmin) }
        }
    }

    @Test
    fun `CA-01_1 el correo no distingue mayusculas`() {
        login("  ADMIN@UrbanMovil.co ", contrasenaAdmin).andExpect { status { isOk() } }
    }

    @Test
    fun `CA-01_2 contrasena incorrecta responde 401 sin indicar que dato fallo`() {
        login(correoAdmin, "otraClave99").andExpect {
            status { isUnauthorized() }
            jsonPath("$.codigo") { value("CREDENCIALES_INVALIDAS") }
            jsonPath("$.mensaje") { value("Credenciales inválidas") }
        }
    }

    @Test
    fun `CA-01_2 correo inexistente responde exactamente igual`() {
        login("nadie@urbanmovil.co", contrasenaAdmin).andExpect {
            status { isUnauthorized() }
            jsonPath("$.codigo") { value("CREDENCIALES_INVALIDAS") }
            jsonPath("$.mensaje") { value("Credenciales inválidas") }
        }
    }

    @Test
    fun `CA-01_3 un conductor no puede iniciar sesion en la web`() {
        insertarConductor(correo = "conductor@urbanmovil.co", contrasena = "Conductor123")

        val resultado = login("conductor@urbanmovil.co", "Conductor123").andExpect {
            status { isUnauthorized() }
            jsonPath("$.codigo") { value("CREDENCIALES_INVALIDAS") }
            jsonPath("$.mensaje") { value("Credenciales inválidas") }
        }.andReturn()

        // No obtiene sesión autenticada
        val sesion = resultado.request.getSession(false) as MockHttpSession?
        mockMvc.get("/api/v1/auth/me") { sesion?.let { session = it } }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `login sin datos responde 400 VALIDACION`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json("correo" to "", "contrasena" to "")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.campos.length()") { value(2) }
        }
    }

    @Test
    fun `sin sesion GET conductores responde 401 JSON`() {
        mockMvc.get("/api/v1/conductores").andExpect {
            status { isUnauthorized() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.codigo") { value("NO_AUTENTICADO") }
            jsonPath("$.mensaje") { value("Debe iniciar sesión para continuar") }
        }
    }

    @Test
    fun `me sin sesion responde 401`() {
        mockMvc.get("/api/v1/auth/me").andExpect {
            status { isUnauthorized() }
            jsonPath("$.codigo") { value("NO_AUTENTICADO") }
        }
    }

    @Test
    fun `logout invalida la sesion`() {
        val sesion = iniciarSesionAdmin()

        mockMvc.post("/api/v1/auth/logout") { session = sesion }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/v1/auth/me") { session = sesion }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `la documentacion OpenAPI es publica`() {
        mockMvc.get("/v3/api-docs").andExpect { status { isOk() } }
    }

    @Test
    fun `Flyway crea las 10 tablas y existe el administrador inicial`() {
        val tablas = jdbc.queryForList(
            """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
            """.trimIndent(),
            String::class.java,
        ).toSet()
        assertEquals(
            setOf(
                "administrador", "conductor", "vehiculo", "ruta", "camino_alterno", "programacion",
                "programacion_ruta", "reasignacion", "reasignacion_ruta", "recorrido",
            ),
            tablas,
        )
        val admins = jdbc.queryForObject(
            "SELECT count(*) FROM administrador WHERE correo = ?", Long::class.javaObjectType, correoAdmin,
        )
        assertEquals(1L, admins)
    }
}
