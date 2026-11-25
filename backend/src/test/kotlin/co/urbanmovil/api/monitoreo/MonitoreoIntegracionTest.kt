package co.urbanmovil.api.monitoreo

import co.urbanmovil.api.ruta.Punto
import co.urbanmovil.api.soporte.OperacionTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import java.time.Duration
import kotlin.math.abs
import kotlin.test.assertTrue

/** HU-31 · §6.3 de la Fase 3. La hora la controla el RelojPrueba. */
class MonitoreoIntegracionTest : OperacionTest() {

    private val url = "/api/v1/monitoreo/buses"

    private var andres = 0L
    private var laura = 0L
    private var r1 = 0L
    private var r2 = 0L
    private var bus104 = 0L
    private var bus305 = 0L

    @BeforeEach
    fun escenario() {
        andres = crearConductor("Andrés Muñoz", 1)
        laura = crearConductor("Laura Idrobo", 2)
        r1 = crearRuta("R1")
        r2 = crearRuta("R2", listOf(2.4300 to -76.6200, 2.4350 to -76.6150, 2.4400 to -76.6100))
        bus104 = crearVehiculo("UM-104", "ABC104")
        bus305 = crearVehiculo("UM-305", "ABC305")
        programar(andres, r1).andExpect { status { isOk() } }
        programar(laura, r2).andExpect { status { isOk() } }
    }

    private fun posicionDel(recorridoId: Long): Punto {
        val respuesta = jsonMapper.readTree(mockMvc.get(url).andReturn().response.contentAsString)
        val bus = respuesta.get("buses").values().first { it.get("recorridoId").asLong() == recorridoId }
        return Punto(bus.get("posicion").get("lat").asDouble(), bus.get("posicion").get("lng").asDouble())
    }

    @Test
    fun `CA-31_3 y CA-31_5 sin recorridos activos responde buses vacio y simulado true`() {
        mockMvc.get(url).andExpect {
            status { isOk() }
            jsonPath("$.simulado") { value(true) }
            jsonPath("$.generadoEn") { value(org.hamcrest.Matchers.startsWith("2026-11-24T10:00")) }
            jsonPath("$.buses") { isEmpty() }
        }
    }

    @Test
    fun `CA-31_1 un elemento por recorrido activo con vehiculo, ruta, conductor, posicion y hora`() {
        val primero = enRecorrido(andres, r1, bus104)
        val segundo = enRecorrido(laura, r2, bus305)

        mockMvc.get(url).andExpect {
            status { isOk() }
            jsonPath("$.simulado") { value(true) }
            jsonPath("$.buses.length()") { value(2) }
            jsonPath("$.buses[0].recorridoId") { value(primero) }
            jsonPath("$.buses[0].vehiculo.id") { value(bus104) }
            jsonPath("$.buses[0].vehiculo.numeroInterno") { value("UM-104") }
            jsonPath("$.buses[0].vehiculo.placa") { value("ABC104") }
            jsonPath("$.buses[0].ruta.id") { value(r1) }
            jsonPath("$.buses[0].ruta.codigo") { value("R1") }
            jsonPath("$.buses[0].ruta.nombre") { value("Ruta R1") }
            jsonPath("$.buses[0].ruta.color") { isString() }
            jsonPath("$.buses[0].conductor.id") { value(andres) }
            jsonPath("$.buses[0].conductor.nombre") { value("Andrés Muñoz") }
            // recién iniciado: está en el primer punto del recorrido principal
            jsonPath("$.buses[0].posicion.lat") { value(2.4419) }
            jsonPath("$.buses[0].posicion.lng") { value(-76.6066) }
            jsonPath("$.buses[0].horaPosicion") { value(org.hamcrest.Matchers.startsWith("2026-11-24T10:00")) }
            jsonPath("$.buses[1].recorridoId") { value(segundo) }
            jsonPath("$.buses[1].posicion.lat") { value(2.43) }
        }
    }

    @Test
    fun `CA-31_2 unos segundos despues la posicion avanzo sobre el recorrido principal`() {
        val id = enRecorrido(andres, r1, bus104)
        reloj.avanzar(Duration.ofSeconds(30))
        val antes = posicionDel(id)
        reloj.avanzar(Duration.ofSeconds(5))
        val despues = posicionDel(id)

        // El trazado sube hacia el noreste: avanzar = más latitud y menos |longitud|
        assertTrue(despues.lat > antes.lat, "La latitud debía crecer: $antes → $despues")
        assertTrue(despues.lng > antes.lng, "La longitud debía crecer: $antes → $despues")
        mockMvc.get(url).andExpect {
            jsonPath("$.buses[0].horaPosicion") { value(org.hamcrest.Matchers.startsWith("2026-11-24T10:00:35")) }
        }
    }

    @Test
    fun `la posicion coincide con el simulador sobre el recorrido principal y no usa alternos`() {
        val conAlterno = postJson(
            "/api/v1/rutas",
            json(
                "codigo" to "R9",
                "nombre" to "Con alterno lejano",
                "recorridoPrincipal" to trazadoBase.map { (lat, lng) -> mapOf("lat" to lat, "lng" to lng) },
                "alternos" to listOf(
                    mapOf("nombre" to "Muy lejos", "puntos" to listOf(mapOf("lat" to 10.0, "lng" to 10.0), mapOf("lat" to 11.0, "lng" to 11.0))),
                ),
            ),
        ).andExpect { status { isCreated() } }.id()
        programar(andres, r1, conAlterno).andExpect { status { isOk() } }
        val id = enRecorrido(andres, conAlterno, bus104)
        val inicio = reloj.instant()
        reloj.avanzar(Duration.ofSeconds(90))

        val esperado = SimuladorPosicion.posicion(trazadoBase.map { (lat, lng) -> Punto(lat, lng) }, inicio, reloj.instant(), 18.0)
        val real = posicionDel(id)
        assertTrue(abs(esperado.lat - real.lat) < 1e-12 && abs(esperado.lng - real.lng) < 1e-12, "$esperado ≠ $real")
    }

    @Test
    fun `CA-31_4 un recorrido finalizado ya no aparece`() {
        val primero = enRecorrido(andres, r1, bus104)
        enRecorrido(laura, r2, bus305)

        finalizar(primero).andExpect { status { isOk() } }

        mockMvc.get(url).andExpect {
            jsonPath("$.buses.length()") { value(1) }
            jsonPath("$.buses[0].conductor.nombre") { value("Laura Idrobo") }
        }
    }

    @Test
    fun `sin sesion responde 401`() {
        mockMvc.get(url) { with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()) }
            .andExpect { status { isUnauthorized() } }
    }
}
