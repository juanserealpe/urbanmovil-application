package co.urbanmovil.api.vehiculo

import co.urbanmovil.api.soporte.IntegracionTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.assertEquals

/** HU-28, HU-29, HU-30. */
@WithMockUser(roles = ["ADMIN"])
class VehiculoIntegracionTest : IntegracionTest() {

    private val base = "/api/v1/vehiculos"

    private fun datos(placa: String? = "abc-123", numeroInterno: String? = "um-104") =
        json("placa" to placa, "numeroInterno" to numeroInterno)

    private fun registrar(cuerpo: String = datos()): ResultActionsDsl = mockMvc.post(base) {
        contentType = MediaType.APPLICATION_JSON
        content = cuerpo
    }

    private fun registrarYObtenerId(cuerpo: String = datos()): Long {
        val respuesta = registrar(cuerpo).andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return jsonMapper.readTree(respuesta).get("id").asLong()
    }

    private fun cambiarEstado(id: Long, cuerpo: String) = mockMvc.patch("$base/$id/estado") {
        contentType = MediaType.APPLICATION_JSON
        content = cuerpo
    }

    // ---------------------------------------------------------------- HU-28

    @Test
    fun `CA-28_1 registra el vehiculo activo y normaliza placa y numero interno`() {
        registrar().andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.placa") { value("ABC123") }
            jsonPath("$.numeroInterno") { value("UM-104") }
            jsonPath("$.activo") { value(true) }
            jsonPath("$.motivoInactivacion") { value(null) }
            jsonPath("$.fechaInactivacion") { value(null) }
            jsonPath("$.enRecorrido") { value(null) }
        }
    }

    @Test
    fun `CA-28_2 placa repetida responde 409 indicando el campo`() {
        registrarYObtenerId()

        registrar(datos(placa = "ABC 123", numeroInterno = "UM-200")).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("DATO_REPETIDO") }
            jsonPath("$.mensaje") { value("Ya existe un vehículo con esta placa") }
            jsonPath("$.campos.length()") { value(1) }
            jsonPath("$.campos[0].campo") { value("placa") }
        }
    }

    @Test
    fun `CA-28_2 numero interno repetido responde 409 indicando el campo`() {
        registrarYObtenerId()

        registrar(datos(placa = "XYZ789", numeroInterno = "UM-104")).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("DATO_REPETIDO") }
            jsonPath("$.mensaje") { value("Ya existe un vehículo con este número interno") }
            jsonPath("$.campos[0].campo") { value("numeroInterno") }
        }
    }

    @Test
    fun `CA-28_2 ambos repetidos se senalan juntos`() {
        registrarYObtenerId()

        registrar(datos()).andExpect {
            status { isConflict() }
            jsonPath("$.campos.length()") { value(2) }
        }
    }

    @Test
    fun `CA-28_3 campo obligatorio vacio responde 400 VALIDACION`() {
        registrar(datos(placa = "")).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("Escriba la placa") }
            jsonPath("$.campos[0].campo") { value("placa") }
        }
        registrar(datos(numeroInterno = null)).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Escriba el número interno") }
            jsonPath("$.campos[0].campo") { value("numeroInterno") }
        }
    }

    @Test
    fun `placa de mas de 10 caracteres responde 400`() {
        registrar(datos(placa = "ABCDEFGHIJK")).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("placa") }
        }
    }

    // ---------------------------------------------------------------- HU-29

    @Test
    fun `CA-29_1 modifica placa y numero interno`() {
        val id = registrarYObtenerId()
        registrarYObtenerId(datos(placa = "OTR999", numeroInterno = "UM-999"))

        mockMvc.put("$base/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = datos(placa = "def-456", numeroInterno = "UM-105")
        }.andExpect {
            status { isOk() }
            jsonPath("$.placa") { value("DEF456") }
            jsonPath("$.numeroInterno") { value("UM-105") }
        }
    }

    @Test
    fun `CA-29_1 puede guardar sus propios valores sin que cuenten como repetidos`() {
        val id = registrarYObtenerId()

        mockMvc.put("$base/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = datos()
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `CA-29_1 modificar a una placa de otro vehiculo responde 409`() {
        val id = registrarYObtenerId()
        registrarYObtenerId(datos(placa = "OTR999", numeroInterno = "UM-999"))

        mockMvc.put("$base/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = datos(placa = "otr-999")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.campos[0].campo") { value("placa") }
        }
    }

    @Test
    fun `CA-29_2 elimina un vehiculo sin recorrido activo`() {
        val id = registrarYObtenerId()
        val finalizado = insertarRecorrido(vehiculoId = id, estado = "FINALIZADO")

        mockMvc.delete("$base/$id").andExpect { status { isNoContent() } }

        mockMvc.get("$base/$id").andExpect {
            status { isNotFound() }
            jsonPath("$.codigo") { value("NO_ENCONTRADO") }
            jsonPath("$.mensaje") { value("El vehículo no existe") }
        }
        val quedan = jdbc.queryForObject(
            "SELECT count(*) FROM recorrido WHERE id = ?", Long::class.javaObjectType, finalizado,
        )
        assertEquals(0L, quedan) // D-17
    }

    @Test
    fun `CA-29_3 no elimina un vehiculo con recorrido activo`() {
        val id = registrarYObtenerId()
        insertarRecorrido(vehiculoId = id)

        mockMvc.delete("$base/$id").andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") {
                value("No se puede eliminar: el vehículo tiene un recorrido activo. Debe finalizarse primero.")
            }
        }
        mockMvc.get("$base/$id").andExpect { status { isOk() } }
    }

    // ---------------------------------------------------------------- HU-30

    @Test
    fun `CA-30_1 desactiva con motivo y registra la fecha en zona -05 00`() {
        val id = registrarYObtenerId()

        cambiarEstado(id, json("activo" to false, "motivo" to "Mantenimiento de frenos")).andExpect {
            status { isOk() }
            jsonPath("$.activo") { value(false) }
            jsonPath("$.motivoInactivacion") { value("Mantenimiento de frenos") }
            jsonPath("$.fechaInactivacion") { value(org.hamcrest.Matchers.endsWith("-05:00")) }
        }
    }

    @Test
    fun `CA-30_2 desactivar sin motivo responde 400`() {
        val id = registrarYObtenerId()

        cambiarEstado(id, json("activo" to false, "motivo" to "   ")).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("Escriba el motivo de la desactivación") }
            jsonPath("$.campos[0].campo") { value("motivo") }
        }
    }

    @Test
    fun `CA-30_3 no desactiva un vehiculo con recorrido activo`() {
        val id = registrarYObtenerId()
        val recorrido = insertarRecorrido(vehiculoId = id, rutaCodigo = "R3")

        cambiarEstado(id, json("activo" to false, "motivo" to "Llanta pinchada")).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") { value("No se puede desactivar mientras tiene un recorrido activo.") }
        }

        mockMvc.get(base).andExpect {
            jsonPath("$[0].activo") { value(true) }
            jsonPath("$[0].enRecorrido.recorridoId") { value(recorrido) }
            jsonPath("$[0].enRecorrido.rutaCodigo") { value("R3") }
        }
    }

    @Test
    fun `CA-30_4 reactiva y limpia motivo y fecha`() {
        val id = registrarYObtenerId()
        cambiarEstado(id, json("activo" to false, "motivo" to "Revisión técnico-mecánica")).andExpect { status { isOk() } }

        cambiarEstado(id, json("activo" to true)).andExpect {
            status { isOk() }
            jsonPath("$.activo") { value(true) }
            jsonPath("$.motivoInactivacion") { value(null) }
            jsonPath("$.fechaInactivacion") { value(null) }
        }
    }

    // ---------------------------------------------------------------- Lista

    @Test
    fun `lista filtra por texto y por estado`() {
        registrarYObtenerId()
        val inactivo = registrarYObtenerId(datos(placa = "XYZ789", numeroInterno = "UM-410"))
        cambiarEstado(inactivo, json("activo" to false, "motivo" to "Mantenimiento")).andExpect { status { isOk() } }

        mockMvc.get(base) { param("activo", "false") }.andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].numeroInterno") { value("UM-410") }
        }
        mockMvc.get(base) { param("q", "abc") }.andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].placa") { value("ABC123") }
        }
        mockMvc.get(base).andExpect { jsonPath("$.length()") { value(2) } }
    }
}
