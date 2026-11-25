package co.urbanmovil.api.soporte

import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * Base para las pruebas de la Fase 3. Arma el escenario pasando por la API real
 * (rutas, conductores, vehículos, programación y recorridos de demo). La sesión simulada es la
 * del administrador inicial, porque las reasignaciones guardan quién las hizo.
 */
@WithMockUser(username = OperacionTest.CORREO_ADMIN, roles = ["ADMIN"])
abstract class OperacionTest : IntegracionTest() {

    /** Trazado de ~1,1 km hacia el noreste del Parque Caldas. */
    protected val trazadoBase = listOf(
        2.4419 to -76.6066,
        2.4440 to -76.6050,
        2.4462 to -76.6031,
        2.4490 to -76.6010,
        2.4510 to -76.5990,
    )

    protected fun postJson(url: String, body: String): ResultActionsDsl = mockMvc.post(url) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    protected fun putJson(url: String, body: String): ResultActionsDsl = mockMvc.put(url) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    protected fun ResultActionsDsl.id(): Long =
        jsonMapper.readTree(andReturn().response.contentAsString).get("id").asLong()

    protected fun crearRuta(codigo: String, trazado: List<Pair<Double, Double>> = trazadoBase): Long =
        postJson(
            "/api/v1/rutas",
            json(
                "codigo" to codigo,
                "nombre" to "Ruta $codigo",
                "recorridoPrincipal" to trazado.map { (lat, lng) -> mapOf("lat" to lat, "lng" to lng) },
            ),
        ).andExpect { status { isCreated() } }.id()

    protected fun crearConductor(nombre: String, numero: Int): Long =
        postJson(
            "/api/v1/conductores",
            json(
                "nombre" to nombre,
                "identificacion" to "9000$numero",
                "correo" to "conductor$numero@prueba.co",
                "telefono" to "300000000$numero",
                "contrasena" to "Conductor123",
            ),
        ).andExpect { status { isCreated() } }.id()

    protected fun crearVehiculo(numeroInterno: String, placa: String): Long =
        postJson("/api/v1/vehiculos", json("placa" to placa, "numeroInterno" to numeroInterno))
            .andExpect { status { isCreated() } }.id()

    protected fun programar(conductorId: Long, vararg rutas: Long): ResultActionsDsl =
        putJson("/api/v1/conductores/$conductorId/programacion", json("rutas" to rutas.toList()))

    protected fun iniciarRecorrido(conductorId: Long, rutaId: Long, vehiculoId: Long): ResultActionsDsl =
        postJson(
            "/api/v1/demo/recorridos",
            json("conductorId" to conductorId, "rutaId" to rutaId, "vehiculoId" to vehiculoId),
        )

    /** Inicia el recorrido (la ruta ya debe estar programada hoy) y devuelve su id. */
    protected fun enRecorrido(conductorId: Long, rutaId: Long, vehiculoId: Long): Long =
        iniciarRecorrido(conductorId, rutaId, vehiculoId).andExpect { status { isCreated() } }.id()

    protected fun finalizar(recorridoId: Long): ResultActionsDsl =
        mockMvc.post("/api/v1/demo/recorridos/$recorridoId/finalizar")

    protected fun reasignar(conductorId: Long, motivo: String?, asignar: List<Long> = emptyList(), quitar: List<Long> = emptyList()) =
        postJson(
            "/api/v1/conductores/$conductorId/reasignaciones",
            json("motivo" to motivo, "rutasAsignar" to asignar, "rutasQuitar" to quitar),
        )

    companion object {
        const val CORREO_ADMIN = "admin@urbanmovil.co"
    }
}
