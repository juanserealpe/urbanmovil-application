package co.urbanmovil.api.ruta

import co.urbanmovil.api.soporte.IntegracionTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

/** Sin sesión: el endpoint público responde y el administrativo no. */
class RutaSeguridadTest : IntegracionTest() {

    @Test
    fun `GET public rutas responde 200 sin sesion`() {
        mockMvc.get("/api/v1/public/rutas").andExpect {
            status { isOk() }
            jsonPath("$") { isArray() }
        }
    }

    @Test
    fun `GET rutas responde 401 sin sesion`() {
        mockMvc.get("/api/v1/rutas").andExpect {
            status { isUnauthorized() }
            jsonPath("$.codigo") { value("NO_AUTENTICADO") }
        }
    }
}
