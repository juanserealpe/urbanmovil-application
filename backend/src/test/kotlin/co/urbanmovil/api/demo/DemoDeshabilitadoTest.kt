package co.urbanmovil.api.demo

import co.urbanmovil.api.soporte.OperacionTest
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals

/**
 * Sin el perfil demo los endpoints de recorridos no existen (404) y no se siembra nada.
 * Usa su propio contexto (otra propiedad), por eso levanta otro contenedor.
 */
@TestPropertySource(properties = ["urbanmovil.demo.habilitado=false"])
class DemoDeshabilitadoTest : OperacionTest() {

    @Test
    fun `los endpoints de demo responden 404`() {
        mockMvc.get("/api/v1/demo/recorridos").andExpect {
            status { isNotFound() }
            jsonPath("$.codigo") { value("NO_ENCONTRADO") }
        }
        postJson("/api/v1/demo/recorridos", json("conductorId" to 1, "rutaId" to 1, "vehiculoId" to 1))
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `el monitoreo sigue disponible y sin buses`() {
        mockMvc.get("/api/v1/monitoreo/buses").andExpect {
            status { isOk() }
            jsonPath("$.buses") { isEmpty() }
        }
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM recorrido", Long::class.javaObjectType))
    }
}
