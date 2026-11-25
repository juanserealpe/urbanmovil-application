package co.urbanmovil.api.demo

import co.urbanmovil.api.soporte.OperacionTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch

/** D-16 · §5 de la Fase 3, y reglas de las fases 1 y 2 de punta a punta con recorridos reales. */
class DemoRecorridoIntegracionTest : OperacionTest() {

    private var andres = 0L
    private var r1 = 0L
    private var r2 = 0L
    private var bus = 0L

    @BeforeEach
    fun escenario() {
        andres = crearConductor("Andrés Muñoz", 1)
        r1 = crearRuta("R1")
        r2 = crearRuta("R2")
        bus = crearVehiculo("UM-104", "ABC104")
        programar(andres, r1, r2).andExpect { status { isOk() } }
    }

    private fun esperarRegla(mensaje: String, accion: () -> org.springframework.test.web.servlet.ResultActionsDsl) {
        accion().andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") { value(mensaje) }
        }
    }

    // ---------------------------------------------------------------- Iniciar y finalizar

    @Test
    fun `inicia un recorrido ACTIVO con inicio ahora`() {
        iniciarRecorrido(andres, r1, bus).andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.estado") { value("ACTIVO") }
            jsonPath("$.conductor.id") { value(andres) }
            jsonPath("$.conductor.nombre") { value("Andrés Muñoz") }
            jsonPath("$.ruta.codigo") { value("R1") }
            jsonPath("$.vehiculo.numeroInterno") { value("UM-104") }
            jsonPath("$.inicio") { value(org.hamcrest.Matchers.startsWith("2026-11-24T10:00")) }
            jsonPath("$.fin") { value(null) }
        }
    }

    @Test
    fun `finaliza un recorrido y no deja finalizarlo dos veces`() {
        val id = enRecorrido(andres, r1, bus)

        finalizar(id).andExpect {
            status { isOk() }
            jsonPath("$.estado") { value("FINALIZADO") }
            jsonPath("$.fin") { isString() }
        }
        esperarRegla("El recorrido ya está finalizado") { finalizar(id) }
    }

    @Test
    fun `finalizar un recorrido que no existe responde 404`() {
        finalizar(9999).andExpect { status { isNotFound() } }
    }

    @Test
    fun `lista recorridos y filtra por estado`() {
        val primero = enRecorrido(andres, r1, bus)
        finalizar(primero).andExpect { status { isOk() } }
        enRecorrido(andres, r2, bus)

        mockMvc.get("/api/v1/demo/recorridos").andExpect { jsonPath("$.length()") { value(2) } }
        mockMvc.get("/api/v1/demo/recorridos") { param("estado", "ACTIVO") }.andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].ruta.codigo") { value("R2") }
        }
    }

    // ---------------------------------------------------------------- Validaciones al iniciar

    @Test
    fun `conductor no disponible no puede iniciar`() {
        patchJson("/api/v1/conductores/$andres/disponibilidad", json("disponible" to false, "motivo" to "PERMISO"))
        esperarRegla("El conductor no está disponible") { iniciarRecorrido(andres, r1, bus) }
    }

    @Test
    fun `ruta fuera de la programacion de hoy no se puede iniciar`() {
        val r3 = crearRuta("R3")
        esperarRegla("La ruta no está en la programación de hoy del conductor") { iniciarRecorrido(andres, r3, bus) }
    }

    @Test
    fun `vehiculo inactivo no se puede usar (RN-25)`() {
        patchJson("/api/v1/vehiculos/$bus/estado", json("activo" to false, "motivo" to "Frenos"))
        esperarRegla("El vehículo está inactivo y no puede usarse en recorridos") { iniciarRecorrido(andres, r1, bus) }
    }

    @Test
    fun `el conductor ya tiene un recorrido activo`() {
        enRecorrido(andres, r1, bus)
        val otroBus = crearVehiculo("UM-118", "ABC118")
        esperarRegla("El conductor ya tiene un recorrido activo") { iniciarRecorrido(andres, r2, otroBus) }
    }

    @Test
    fun `el vehiculo ya esta en otro recorrido activo`() {
        enRecorrido(andres, r1, bus)
        val laura = crearConductor("Laura Idrobo", 2)
        programar(laura, r2).andExpect { status { isOk() } }
        esperarRegla("El vehículo ya está en otro recorrido activo") { iniciarRecorrido(laura, r2, bus) }
    }

    @Test
    fun `datos faltantes responden 400 e ids inexistentes 404`() {
        postJson("/api/v1/demo/recorridos", json("conductorId" to andres, "rutaId" to r1)).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("vehiculoId") }
        }
        iniciarRecorrido(andres, r1, 9999).andExpect { status { isNotFound() } }
    }

    // ---------------------------------------------------------------- Reglas de punta a punta

    @Test
    fun `D-05 no se elimina un conductor en recorrido (CA-07_3) y al finalizar si`() {
        val id = enRecorrido(andres, r1, bus)
        esperarRegla("No se puede eliminar: el conductor tiene un recorrido activo. Debe finalizarlo primero.") {
            mockMvc.delete("/api/v1/conductores/$andres")
        }
        finalizar(id).andExpect { status { isOk() } }
        mockMvc.delete("/api/v1/conductores/$andres").andExpect { status { isNoContent() } }
    }

    @Test
    fun `D-05 no se elimina una ruta con recorrido activo (CA-05_2) y al finalizar si`() {
        val id = enRecorrido(andres, r1, bus)
        esperarRegla("No se puede eliminar: la ruta tiene recorridos activos.") { mockMvc.delete("/api/v1/rutas/$r1") }
        finalizar(id).andExpect { status { isOk() } }
        mockMvc.delete("/api/v1/rutas/$r1").andExpect { status { isNoContent() } }
    }

    @Test
    fun `D-08 no se marca no disponible a un conductor en recorrido (CA-08_4)`() {
        val id = enRecorrido(andres, r1, bus)
        esperarRegla("No se puede marcar como no disponible mientras tiene un recorrido activo.") {
            patchJson("/api/v1/conductores/$andres/disponibilidad", json("disponible" to false, "motivo" to "PERMISO"))
        }
        finalizar(id).andExpect { status { isOk() } }
        patchJson("/api/v1/conductores/$andres/disponibilidad", json("disponible" to false, "motivo" to "PERMISO"))
            .andExpect { status { isOk() } }
    }

    @Test
    fun `RN-26 no se elimina ni desactiva un vehiculo en recorrido (CA-29_3 y CA-30_3)`() {
        val id = enRecorrido(andres, r1, bus)
        esperarRegla("No se puede eliminar: el vehículo tiene un recorrido activo. Debe finalizarse primero.") {
            mockMvc.delete("/api/v1/vehiculos/$bus")
        }
        esperarRegla("No se puede desactivar mientras tiene un recorrido activo.") {
            patchJson("/api/v1/vehiculos/$bus/estado", json("activo" to false, "motivo" to "Frenos"))
        }
        mockMvc.get("/api/v1/vehiculos/$bus").andExpect { jsonPath("$.enRecorrido.recorridoId") { value(id) } }

        finalizar(id).andExpect { status { isOk() } }
        patchJson("/api/v1/vehiculos/$bus/estado", json("activo" to false, "motivo" to "Frenos")).andExpect { status { isOk() } }
        mockMvc.delete("/api/v1/vehiculos/$bus").andExpect { status { isNoContent() } }
    }

    private fun patchJson(url: String, body: String) = mockMvc.patch(url) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }
}
