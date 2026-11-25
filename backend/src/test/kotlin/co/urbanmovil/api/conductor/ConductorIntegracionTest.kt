package co.urbanmovil.api.conductor

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

/** HU-06, HU-07, HU-08. */
@WithMockUser(roles = ["ADMIN"])
class ConductorIntegracionTest : IntegracionTest() {

    private val base = "/api/v1/conductores"

    private fun datosConductor(
        nombre: String? = "Juan Pérez",
        identificacion: String? = "1061234567",
        correo: String? = "juan.perez@urbanmovil.co",
        telefono: String? = "3001234567",
        contrasena: String? = "Secreta123",
    ) = json(
        "nombre" to nombre,
        "identificacion" to identificacion,
        "correo" to correo,
        "telefono" to telefono,
        "contrasena" to contrasena,
    )

    private fun registrar(cuerpo: String = datosConductor()): ResultActionsDsl = mockMvc.post(base) {
        contentType = MediaType.APPLICATION_JSON
        content = cuerpo
    }

    private fun registrarYObtenerId(cuerpo: String = datosConductor()): Long {
        val respuesta = registrar(cuerpo).andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return jsonMapper.readTree(respuesta).get("id").asLong()
    }

    private fun cambiarDisponibilidad(id: Long, cuerpo: String) = mockMvc.patch("$base/$id/disponibilidad") {
        contentType = MediaType.APPLICATION_JSON
        content = cuerpo
    }

    // ---------------------------------------------------------------- HU-06

    @Test
    fun `CA-06_1 registra el conductor y queda disponible`() {
        registrar().andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.nombre") { value("Juan Pérez") }
            jsonPath("$.identificacion") { value("1061234567") }
            jsonPath("$.correo") { value("juan.perez@urbanmovil.co") }
            jsonPath("$.telefono") { value("3001234567") }
            jsonPath("$.disponible") { value(true) }
            jsonPath("$.motivoNoDisponible") { value(null) }
            jsonPath("$.enRecorrido") { value(null) }
            jsonPath("$.rutasHoy") { isEmpty() }
            jsonPath("$.contrasena") { doesNotExist() }
            jsonPath("$.hashContrasena") { doesNotExist() }
        }
    }

    @Test
    fun `CA-06_1 recorta espacios y guarda la contrasena cifrada`() {
        val id = registrarYObtenerId(datosConductor(nombre = "  Juan Pérez  ", correo = " Juan.Perez@UrbanMovil.co "))

        mockMvc.get("$base/$id").andExpect {
            status { isOk() }
            jsonPath("$.nombre") { value("Juan Pérez") }
            jsonPath("$.correo") { value("juan.perez@urbanmovil.co") }
        }
        val hash = jdbc.queryForObject("SELECT hash_contrasena FROM conductor WHERE id = ?", String::class.java, id)!!
        assertEquals(true, passwordEncoder.matches("Secreta123", hash))
    }

    @Test
    fun `CA-06_2 correo repetido responde 409 DATO_REPETIDO indicando el campo`() {
        registrarYObtenerId()

        registrar(datosConductor(identificacion = "999", correo = "JUAN.PEREZ@urbanmovil.co")).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("DATO_REPETIDO") }
            jsonPath("$.mensaje") { value("Ya existe un conductor con este correo") }
            jsonPath("$.campos.length()") { value(1) }
            jsonPath("$.campos[0].campo") { value("correo") }
        }
    }

    @Test
    fun `CA-06_2 identificacion repetida responde 409 DATO_REPETIDO indicando el campo`() {
        registrarYObtenerId()

        registrar(datosConductor(correo = "otro@urbanmovil.co")).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("DATO_REPETIDO") }
            jsonPath("$.mensaje") { value("Ya existe un conductor con este número de identificación") }
            jsonPath("$.campos[0].campo") { value("identificacion") }
        }
    }

    @Test
    fun `CA-06_2 al modificar a un correo ya usado por otro conductor responde 409`() {
        registrarYObtenerId()
        val otro = registrarYObtenerId(datosConductor(identificacion = "222", correo = "otro@urbanmovil.co"))

        mockMvc.put("$base/$otro") {
            contentType = MediaType.APPLICATION_JSON
            content = datosConductor(identificacion = "222", correo = "juan.perez@urbanmovil.co", contrasena = null)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("DATO_REPETIDO") }
            jsonPath("$.campos[0].campo") { value("correo") }
        }
    }

    @Test
    fun `CA-06_3 campo obligatorio vacio responde 400 VALIDACION senalando el campo`() {
        registrar(datosConductor(nombre = "   ")).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("Escriba el nombre") }
            jsonPath("$.campos[0].campo") { value("nombre") }
        }
    }

    @Test
    fun `CA-06_3 campo obligatorio ausente y correo invalido`() {
        registrar(datosConductor(telefono = null)).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("telefono") }
        }
        registrar(datosConductor(correo = "no-es-correo")).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Escriba un correo válido") }
            jsonPath("$.campos[0].campo") { value("correo") }
        }
    }

    @Test
    fun `contrasena de menos de 8 caracteres responde 400`() {
        registrar(datosConductor(contrasena = "corta")).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.campos[0].campo") { value("contrasena") }
        }
    }

    // ---------------------------------------------------------------- HU-07

    @Test
    fun `CA-07_1 modifica los datos de un conductor sin recorrido activo`() {
        val id = registrarYObtenerId()

        mockMvc.put("$base/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = datosConductor(nombre = "Juan Pérez Gómez", telefono = "3109998877", contrasena = null)
        }.andExpect {
            status { isOk() }
            jsonPath("$.nombre") { value("Juan Pérez Gómez") }
            jsonPath("$.telefono") { value("3109998877") }
            jsonPath("$.disponible") { value(true) }
        }
    }

    @Test
    fun `CA-07_1 si llega contrasena al modificar se reemplaza`() {
        val id = registrarYObtenerId()

        mockMvc.put("$base/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = datosConductor(contrasena = "NuevaClave456")
        }.andExpect { status { isOk() } }

        val hash = jdbc.queryForObject("SELECT hash_contrasena FROM conductor WHERE id = ?", String::class.java, id)!!
        assertEquals(true, passwordEncoder.matches("NuevaClave456", hash))
    }

    @Test
    fun `CA-07_2 elimina un conductor sin recorrido activo y borra su historial`() {
        val id = registrarYObtenerId()
        val recorridoFinalizado = insertarRecorrido(conductorId = id, estado = "FINALIZADO")

        mockMvc.delete("$base/$id").andExpect { status { isNoContent() } }

        mockMvc.get("$base/$id").andExpect {
            status { isNotFound() }
            jsonPath("$.codigo") { value("NO_ENCONTRADO") }
        }
        val quedan = jdbc.queryForObject(
            "SELECT count(*) FROM recorrido WHERE id = ?", Long::class.javaObjectType, recorridoFinalizado,
        )
        assertEquals(0L, quedan) // D-17
    }

    @Test
    fun `CA-07_3 no elimina un conductor con recorrido activo`() {
        val id = registrarYObtenerId()
        insertarRecorrido(conductorId = id)

        mockMvc.delete("$base/$id").andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") {
                value("No se puede eliminar: el conductor tiene un recorrido activo. Debe finalizarlo primero.")
            }
        }
        mockMvc.get("$base/$id").andExpect { status { isOk() } }
    }

    @Test
    fun `conductor inexistente responde 404`() {
        mockMvc.get("$base/9999").andExpect {
            status { isNotFound() }
            jsonPath("$.codigo") { value("NO_ENCONTRADO") }
            jsonPath("$.mensaje") { value("El conductor no existe") }
        }
    }

    // ---------------------------------------------------------------- HU-08

    @Test
    fun `CA-08_1 marca no disponible por vacaciones`() {
        val id = registrarYObtenerId()

        cambiarDisponibilidad(id, json("disponible" to false, "motivo" to "VACACIONES", "detalle" to "Del 25 nov al 9 dic"))
            .andExpect {
                status { isOk() }
                jsonPath("$.disponible") { value(false) }
                jsonPath("$.motivoNoDisponible") { value("VACACIONES") }
                jsonPath("$.detalleMotivo") { value("Del 25 nov al 9 dic") }
            }
    }

    @Test
    fun `CA-08_2 marcar no disponible sin motivo responde 400 Seleccione el motivo`() {
        val id = registrarYObtenerId()

        cambiarDisponibilidad(id, json("disponible" to false)).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("Seleccione el motivo") }
            jsonPath("$.campos[0].campo") { value("motivo") }
        }
    }

    @Test
    fun `CA-08_3 marcar disponible limpia motivo y detalle`() {
        val id = registrarYObtenerId()
        cambiarDisponibilidad(id, json("disponible" to false, "motivo" to "PERMISO", "detalle" to "Cita médica"))
            .andExpect { status { isOk() } }

        cambiarDisponibilidad(id, json("disponible" to true)).andExpect {
            status { isOk() }
            jsonPath("$.disponible") { value(true) }
            jsonPath("$.motivoNoDisponible") { value(null) }
            jsonPath("$.detalleMotivo") { value(null) }
        }
    }

    @Test
    fun `CA-08_4 no marca no disponible con recorrido activo`() {
        val id = registrarYObtenerId()
        val recorrido = insertarRecorrido(conductorId = id, rutaCodigo = "R7")

        cambiarDisponibilidad(id, json("disponible" to false, "motivo" to "INCAPACIDAD")).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") { value("No se puede marcar como no disponible mientras tiene un recorrido activo.") }
        }

        // y la lista refleja el recorrido en curso
        mockMvc.get("$base/$id").andExpect {
            jsonPath("$.disponible") { value(true) }
            jsonPath("$.enRecorrido.recorridoId") { value(recorrido) }
            jsonPath("$.enRecorrido.rutaCodigo") { value("R7") }
        }
    }

    @Test
    fun `V-6 motivo OTRO sin detalle responde 400 Describa el motivo`() {
        val id = registrarYObtenerId()

        cambiarDisponibilidad(id, json("disponible" to false, "motivo" to "OTRO", "detalle" to "  ")).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("Describa el motivo") }
            jsonPath("$.campos[0].campo") { value("detalle") }
        }
    }

    @Test
    fun `motivo desconocido responde 400 VALIDACION`() {
        val id = registrarYObtenerId()

        cambiarDisponibilidad(id, json("disponible" to false, "motivo" to "PASEO")).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.campos[0].campo") { value("motivo") }
        }
    }

    // ---------------------------------------------------------------- Lista

    @Test
    fun `lista filtra por texto y por disponibilidad`() {
        registrarYObtenerId()
        val maria = registrarYObtenerId(
            datosConductor(nombre = "María López", identificacion = "555", correo = "maria@urbanmovil.co"),
        )
        cambiarDisponibilidad(maria, json("disponible" to false, "motivo" to "VACACIONES")).andExpect { status { isOk() } }

        mockMvc.get(base) { param("q", "MARÍA") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].nombre") { value("María López") }
        }
        mockMvc.get(base) { param("q", "1061") }.andExpect { jsonPath("$.length()") { value(1) } }
        mockMvc.get(base) { param("disponible", "true") }.andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].nombre") { value("Juan Pérez") }
        }
        mockMvc.get(base).andExpect { jsonPath("$.length()") { value(2) } }
    }
}
