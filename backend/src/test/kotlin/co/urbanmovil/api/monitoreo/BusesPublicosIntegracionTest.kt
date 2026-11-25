package co.urbanmovil.api.monitoreo

import co.urbanmovil.api.soporte.OperacionTest
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous
import org.springframework.test.web.servlet.get
import java.time.Duration

/**
 * Lo que consume la app del pasajero: la misma simulación que el panel, sin datos personales.
 * Con `inicio` + `velocidadKmh` + `generadoEn` cada cliente calcula la posición por su cuenta.
 */
class BusesPublicosIntegracionTest : OperacionTest() {

    private val url = "/api/v1/public/buses"

    private var andres = 0L
    private var r1 = 0L
    private var bus104 = 0L

    @BeforeEach
    fun escenario() {
        andres = crearConductor("Andrés Muñoz", 1)
        r1 = crearRuta("R1")
        bus104 = crearVehiculo("UM-104", "ABC104")
        programar(andres, r1).andExpect { status { isOk() } }
    }

    @Test
    fun `sin sesion responde 200 y buses vacio cuando no hay recorridos`() {
        mockMvc.get(url) { with(anonymous()) }.andExpect {
            status { isOk() }
            jsonPath("$.simulado") { value(true) }
            jsonPath("$.velocidadKmh") { value(18.0) }
            jsonPath("$.generadoEn") { value(startsWith("2026-11-24T10:00")) }
            jsonPath("$.buses") { isEmpty() }
        }
    }

    @Test
    fun `un recorrido activo trae lo necesario para simularlo y nada personal`() {
        val id = enRecorrido(andres, r1, bus104)
        reloj.avanzar(Duration.ofSeconds(40))

        mockMvc.get(url) { with(anonymous()) }.andExpect {
            status { isOk() }
            jsonPath("$.buses.length()") { value(1) }
            jsonPath("$.buses[0].recorridoId") { value(id) }
            jsonPath("$.buses[0].numeroInterno") { value("UM-104") }
            jsonPath("$.buses[0].rutaCodigo") { value("R1") }
            jsonPath("$.buses[0].inicio") { value(startsWith("2026-11-24T10:00:00")) }
            jsonPath("$.buses[0].posicion.lat") { isNumber() }
            jsonPath("$.generadoEn") { value(startsWith("2026-11-24T10:00:40")) }
            // Privacidad: el pasajero no ve quién conduce ni la placa.
            jsonPath("$.buses[0].conductor") { doesNotExist() }
            jsonPath("$.buses[0].placa") { doesNotExist() }
            jsonPath("$.buses[0].vehiculo") { doesNotExist() }
        }
    }

    @Test
    fun `la posicion publica es la misma que ve el panel`() {
        enRecorrido(andres, r1, bus104)
        reloj.avanzar(Duration.ofSeconds(75))

        val panel = jsonMapper.readTree(mockMvc.get("/api/v1/monitoreo/buses").andReturn().response.contentAsString)
        val publico = jsonMapper.readTree(mockMvc.get(url) { with(anonymous()) }.andReturn().response.contentAsString)

        val enPanel = panel.get("buses").get(0).get("posicion")
        val enPublico = publico.get("buses").get(0).get("posicion")
        kotlin.test.assertEquals(enPanel, enPublico)
    }

    @Test
    fun `un recorrido finalizado desaparece de la vista publica`() {
        val id = enRecorrido(andres, r1, bus104)
        finalizar(id).andExpect { status { isOk() } }

        mockMvc.get(url) { with(anonymous()) }.andExpect { jsonPath("$.buses") { isEmpty() } }
    }
}
