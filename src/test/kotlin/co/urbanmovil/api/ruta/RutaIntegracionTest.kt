package co.urbanmovil.api.ruta

import co.urbanmovil.api.soporte.IntegracionTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.assertEquals

/** HU-02, HU-03, HU-04, HU-05. */
@WithMockUser(roles = ["ADMIN"])
class RutaIntegracionTest : IntegracionTest() {

    private val base = "/api/v1/rutas"
    private val publico = "/api/v1/public/rutas"

    private val trazado = listOf(2.4419 to -76.6066, 2.4436 to -76.6049, 2.4462 to -76.6031)
    private val desvio = listOf(2.4419 to -76.6066, 2.4440 to -76.6085, 2.4462 to -76.6031)

    private fun puntos(lista: List<Pair<Double, Double>>) = lista.map { (lat, lng) -> mapOf("lat" to lat, "lng" to lng) }

    private fun alterno(nombre: String, lista: List<Pair<Double, Double>> = desvio, id: Long? = null) =
        buildMap<String, Any> {
            if (id != null) put("id", id)
            put("nombre", nombre)
            put("puntos", puntos(lista))
        }

    private fun cuerpo(
        codigo: String? = "r1",
        nombre: String? = "Centro – Campanario",
        color: String? = null,
        recorrido: List<Pair<Double, Double>>? = trazado,
        alternos: List<Map<String, Any>>? = null,
    ) = json(
        "codigo" to codigo,
        "nombre" to nombre,
        "color" to color,
        "recorridoPrincipal" to recorrido?.let(::puntos),
        "alternos" to alternos,
    )

    private fun registrar(body: String = cuerpo()): ResultActionsDsl = mockMvc.post(base) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun modificar(id: Long, body: String): ResultActionsDsl = mockMvc.put("$base/$id") {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun registrarYObtenerId(body: String = cuerpo()): Long {
        val respuesta = registrar(body).andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return jsonMapper.readTree(respuesta).get("id").asLong()
    }

    private fun idsAlternos(rutaId: Long): List<Long> {
        val respuesta = mockMvc.get("$base/$rutaId").andReturn().response.contentAsString
        return jsonMapper.readTree(respuesta).get("alternos").values().map { it.get("id").asLong() }
    }

    private fun contar(sql: String, vararg args: Any): Long =
        jdbc.queryForObject(sql, Long::class.javaObjectType, *args)!!

    // ---------------------------------------------------------------- HU-02

    @Test
    fun `CA-02_1 registra la ruta y aparece en el endpoint publico`() {
        registrar().andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.codigo") { value("R1") }
            jsonPath("$.nombre") { value("Centro – Campanario") }
            jsonPath("$.puntosRecorrido") { value(3) }
            jsonPath("$.numAlternos") { value(0) }
            jsonPath("$.recorridosActivos") { value(0) }
            jsonPath("$.conductoresHoy") { isEmpty() }
            jsonPath("$.actualizadoEn") { value(org.hamcrest.Matchers.endsWith("-05:00")) }
        }

        mockMvc.get(publico).andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].codigo") { value("R1") }
            jsonPath("$[0].recorridoPrincipal.length()") { value(3) }
        }
    }

    @Test
    fun `CA-02_2 sin recorrido principal responde 400 Falta trazar el recorrido principal`() {
        listOf(cuerpo(recorrido = null), cuerpo(recorrido = emptyList())).forEach { body ->
            registrar(body).andExpect {
                status { isBadRequest() }
                jsonPath("$.codigo") { value("VALIDACION") }
                jsonPath("$.mensaje") { value("Falta trazar el recorrido principal") }
                jsonPath("$.campos[0].campo") { value("recorridoPrincipal") }
            }
        }
        assertEquals(0L, contar("SELECT count(*) FROM ruta"))
    }

    @Test
    fun `CA-02_3 nombre repetido responde 409 DATO_REPETIDO`() {
        registrarYObtenerId()

        registrar(cuerpo(codigo = "R2", nombre = "CENTRO – campanario")).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("DATO_REPETIDO") }
            jsonPath("$.mensaje") { value("Ya existe una ruta con este nombre") }
            jsonPath("$.campos[0].campo") { value("nombre") }
        }
    }

    @Test
    fun `codigo repetido responde 409 DATO_REPETIDO`() {
        registrarYObtenerId()

        registrar(cuerpo(codigo = "R1", nombre = "Otra ruta")).andExpect {
            status { isConflict() }
            jsonPath("$.mensaje") { value("Ya existe una ruta con este código") }
            jsonPath("$.campos[0].campo") { value("codigo") }
        }
    }

    @Test
    fun `CA-02_4 recorrido con un solo punto responde 400`() {
        registrar(cuerpo(recorrido = listOf(2.4419 to -76.6066))).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("La ruta debe tener punto de inicio y punto final") }
            jsonPath("$.campos[0].campo") { value("recorridoPrincipal") }
        }
    }

    @Test
    fun `CA-02_5 los puntos vuelven en el mismo orden con inicio y final`() {
        val n = (0 until 25).map { i -> (2.43 + i * 0.001) to (-76.62 + i * 0.0005) }
        val id = registrarYObtenerId(cuerpo(recorrido = n))

        mockMvc.get("$base/$id").andExpect {
            status { isOk() }
            jsonPath("$.recorridoPrincipal.length()") { value(25) }
            n.forEachIndexed { i, (lat, lng) ->
                jsonPath("$.recorridoPrincipal[$i].lat") { value(lat) }
                jsonPath("$.recorridoPrincipal[$i].lng") { value(lng) }
            }
            jsonPath("$.inicio.lat") { value(n.first().first) }
            jsonPath("$.inicio.lng") { value(n.first().second) }
            jsonPath("$.final.lat") { value(n.last().first) }
            jsonPath("$.final.lng") { value(n.last().second) }
        }
    }

    @Test
    fun `ida y vuelta del JSON del trazado conserva orden y precision`() {
        val precisos = listOf(
            2.441912345678901 to -76.606612345678901,
            2.4 to -76.0,
            -0.000001 to 179.999999,
            89.123456789 to -179.987654321,
        )
        val id = registrarYObtenerId(cuerpo(recorrido = precisos))

        val respuesta = mockMvc.get("$base/$id").andReturn().response.contentAsString
        val leidos = jsonMapper.readTree(respuesta).get("recorridoPrincipal").values().map { it.get("lat").asDouble() to it.get("lng").asDouble() }
        assertEquals(precisos, leidos)

        // y en la BD queda como arreglo JSON de {lat, lng}
        val tipo = jdbc.queryForObject("SELECT jsonb_typeof(recorrido_principal) FROM ruta WHERE id = ?", String::class.java, id)
        assertEquals("array", tipo)
        val primerLat = jdbc.queryForObject("SELECT (recorrido_principal->0->>'lat')::float8 FROM ruta WHERE id = ?", Double::class.javaObjectType, id)
        assertEquals(precisos.first().first, primerLat)
    }

    @Test
    fun `coordenadas fuera de rango responden 400 Coordenadas invalidas`() {
        registrar(cuerpo(recorrido = listOf(2.44 to -76.6, 95.0 to -76.6))).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Coordenadas inválidas") }
            jsonPath("$.campos[0].campo") { value("recorridoPrincipal") }
        }
        registrar(cuerpo(recorrido = listOf(2.44 to -76.6, 2.45 to -181.0))).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Coordenadas inválidas") }
        }
    }

    @Test
    fun `punto sin lng responde 400 Coordenadas invalidas`() {
        val body = json(
            "codigo" to "R1",
            "nombre" to "Ruta",
            "recorridoPrincipal" to listOf(mapOf("lat" to 2.44, "lng" to -76.6), mapOf("lat" to 2.45)),
        )
        registrar(body).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Coordenadas inválidas") }
        }
    }

    @Test
    fun `mas de 2000 puntos responde 400`() {
        val muchos = (0..2_000).map { i -> 2.4 + i * 0.00001 to -76.6 }
        registrar(cuerpo(recorrido = muchos)).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("El trazado supera el máximo de puntos permitido") }
        }
    }

    @Test
    fun `sin color se asigna uno de la paleta y un color mal escrito responde 400`() {
        registrar(cuerpo()).andExpect { jsonPath("$.color") { value(RutaService.PALETA[0]) } }
        registrar(cuerpo(codigo = "R2", nombre = "Segunda")).andExpect { jsonPath("$.color") { value(RutaService.PALETA[1]) } }
        registrar(cuerpo(codigo = "R3", nombre = "Tercera", color = "#abc123")).andExpect { jsonPath("$.color") { value("#ABC123") } }

        registrar(cuerpo(codigo = "R4", nombre = "Cuarta", color = "azul")).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Color inválido") }
            jsonPath("$.campos[0].campo") { value("color") }
        }
    }

    @Test
    fun `codigo y nombre obligatorios`() {
        registrar(cuerpo(codigo = " ")).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("codigo") }
        }
        registrar(cuerpo(nombre = null)).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("nombre") }
        }
    }

    // ---------------------------------------------------------------- HU-03

    @Test
    fun `CA-03_1 el alterno queda asociado y vuelve en el detalle y en public`() {
        val id = registrarYObtenerId(cuerpo(alternos = listOf(alterno("Desvío por Cra 9"))))

        mockMvc.get("$base/$id").andExpect {
            jsonPath("$.numAlternos") { value(1) }
            jsonPath("$.alternos[0].id") { isNumber() }
            jsonPath("$.alternos[0].nombre") { value("Desvío por Cra 9") }
            jsonPath("$.alternos[0].puntos.length()") { value(3) }
            jsonPath("$.alternos[0].puntos[1].lng") { value(-76.6085) }
        }
        mockMvc.get(publico).andExpect {
            jsonPath("$[0].alternos[0].nombre") { value("Desvío por Cra 9") }
            jsonPath("$[0].alternos[0].puntos.length()") { value(3) }
            jsonPath("$[0].alternos[0].id") { doesNotExist() }
        }
    }

    @Test
    fun `CA-03_2 vuelven todos los alternos separados del principal`() {
        val id = registrarYObtenerId(
            cuerpo(alternos = listOf(alterno("Alterno A"), alterno("Alterno B", listOf(1.0 to 1.0, 2.0 to 2.0)))),
        )

        mockMvc.get("$base/$id").andExpect {
            jsonPath("$.recorridoPrincipal.length()") { value(3) }
            jsonPath("$.alternos.length()") { value(2) }
            jsonPath("$.alternos[0].nombre") { value("Alterno A") }
            jsonPath("$.alternos[1].nombre") { value("Alterno B") }
            jsonPath("$.alternos[1].puntos.length()") { value(2) }
        }
        mockMvc.get(publico).andExpect { jsonPath("$[0].alternos.length()") { value(2) } }
    }

    @Test
    fun `alterno con un solo punto responde 400 en su campo`() {
        val alternos = listOf(alterno("Bien"), alterno("Mal", listOf(2.44 to -76.6)))
        registrar(cuerpo(alternos = alternos)).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Cada camino alterno debe tener al menos dos puntos") }
            jsonPath("$.campos[0].campo") { value("alternos[1].puntos") }
        }
    }

    @Test
    fun `alterno sin nombre responde 400 en su campo`() {
        registrar(cuerpo(alternos = listOf(alterno("  ")))).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("alternos[0].nombre") }
        }
    }

    @Test
    fun `mas de 10 alternos responde 400`() {
        registrar(cuerpo(alternos = (1..11).map { alterno("Alterno $it") })).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("alternos") }
        }
    }

    // ---------------------------------------------------------------- HU-04

    @Test
    fun `CA-04_1 modificar nombre y trazado se refleja en public`() {
        val id = registrarYObtenerId()
        val nuevo = listOf(2.45 to -76.60, 2.46 to -76.59, 2.47 to -76.58, 2.48 to -76.57)

        modificar(id, cuerpo(codigo = "R1", nombre = "Centro – Campanario (nuevo)", recorrido = nuevo)).andExpect {
            status { isOk() }
            jsonPath("$.nombre") { value("Centro – Campanario (nuevo)") }
            jsonPath("$.puntosRecorrido") { value(4) }
            jsonPath("$.final.lat") { value(2.48) }
        }
        mockMvc.get(publico).andExpect {
            jsonPath("$[0].nombre") { value("Centro – Campanario (nuevo)") }
            jsonPath("$[0].recorridoPrincipal.length()") { value(4) }
            jsonPath("$[0].recorridoPrincipal[0].lat") { value(2.45) }
        }
    }

    @Test
    fun `CA-04_1 puede guardar su propio nombre y codigo sin que cuenten como repetidos`() {
        val id = registrarYObtenerId()
        modificar(id, cuerpo()).andExpect { status { isOk() } }
    }

    @Test
    fun `CA-04_2 PUT sin recorrido principal responde 400 y no cambia nada`() {
        val id = registrarYObtenerId(cuerpo(alternos = listOf(alterno("Desvío"))))

        modificar(id, cuerpo(nombre = "Nombre que no debe quedar", recorrido = null)).andExpect {
            status { isBadRequest() }
            jsonPath("$.mensaje") { value("Falta trazar el recorrido principal") }
        }
        mockMvc.get("$base/$id").andExpect {
            jsonPath("$.nombre") { value("Centro – Campanario") }
            jsonPath("$.recorridoPrincipal.length()") { value(3) }
            jsonPath("$.alternos.length()") { value(1) }
        }
    }

    @Test
    fun `CA-04_3 PUT con un solo alterno elimina el otro y conserva el enviado`() {
        val id = registrarYObtenerId(cuerpo(alternos = listOf(alterno("Alterno A"), alterno("Alterno B"))))
        val (idA, idB) = idsAlternos(id)

        modificar(id, cuerpo(alternos = listOf(alterno("Alterno B renombrado", id = idB)))).andExpect {
            status { isOk() }
            jsonPath("$.alternos.length()") { value(1) }
            jsonPath("$.alternos[0].id") { value(idB) }
            jsonPath("$.alternos[0].nombre") { value("Alterno B renombrado") }
        }
        assertEquals(0L, contar("SELECT count(*) FROM camino_alterno WHERE id = ?", idA))
        assertEquals(1L, contar("SELECT count(*) FROM camino_alterno WHERE ruta_id = ?", id))
    }

    @Test
    fun `PUT crea los alternos que llegan sin id`() {
        val id = registrarYObtenerId(cuerpo(alternos = listOf(alterno("Alterno A"))))
        val (idA) = idsAlternos(id)

        modificar(id, cuerpo(alternos = listOf(alterno("Alterno A", id = idA), alterno("Alterno nuevo")))).andExpect {
            status { isOk() }
            jsonPath("$.alternos.length()") { value(2) }
            jsonPath("$.alternos[0].id") { value(idA) }
            jsonPath("$.alternos[1].nombre") { value("Alterno nuevo") }
        }
    }

    @Test
    fun `PUT con id de alterno de otra ruta responde 400`() {
        val otra = registrarYObtenerId(cuerpo(codigo = "R2", nombre = "Otra", alternos = listOf(alterno("Ajeno"))))
        val (ajeno) = idsAlternos(otra)
        val id = registrarYObtenerId()

        modificar(id, cuerpo(alternos = listOf(alterno("Robado", id = ajeno)))).andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("alternos[0].id") }
        }
    }

    @Test
    fun `PUT a un nombre de otra ruta responde 409`() {
        registrarYObtenerId()
        val id = registrarYObtenerId(cuerpo(codigo = "R2", nombre = "Otra"))

        modificar(id, cuerpo(codigo = "R2", nombre = "centro – campanario")).andExpect {
            status { isConflict() }
            jsonPath("$.campos[0].campo") { value("nombre") }
        }
    }

    // ---------------------------------------------------------------- HU-05

    @Test
    fun `CA-05_1 elimina la ruta y todo lo asociado`() {
        val id = registrarYObtenerId(cuerpo(alternos = listOf(alterno("Desvío"))))
        val conductor = insertarConductor()
        insertarRecorrido(conductorId = conductor, estado = "FINALIZADO", rutaCodigo = "R1")
        val programacion = contar("INSERT INTO programacion (conductor_id, fecha) VALUES (?, current_date) RETURNING id", conductor)
        jdbc.update("INSERT INTO programacion_ruta (programacion_id, ruta_id, orden) VALUES (?, ?, 1)", programacion, id)
        val admin = contar("SELECT min(id) FROM administrador")
        val reasignacion = contar(
            "INSERT INTO reasignacion (conductor_id, administrador_id, motivo) VALUES (?, ?, 'Prueba') RETURNING id",
            conductor, admin,
        )
        jdbc.update("INSERT INTO reasignacion_ruta (reasignacion_id, ruta_id, tipo) VALUES (?, ?, 'ASIGNADA')", reasignacion, id)

        mockMvc.delete("$base/$id").andExpect { status { isNoContent() } }

        mockMvc.get("$base/$id").andExpect {
            status { isNotFound() }
            jsonPath("$.mensaje") { value("La ruta no existe") }
        }
        mockMvc.get(base).andExpect { jsonPath("$.length()") { value(0) } }
        mockMvc.get(publico).andExpect { jsonPath("$.length()") { value(0) } }
        assertEquals(0L, contar("SELECT count(*) FROM camino_alterno WHERE ruta_id = ?", id))
        assertEquals(0L, contar("SELECT count(*) FROM programacion_ruta WHERE ruta_id = ?", id))
        assertEquals(0L, contar("SELECT count(*) FROM recorrido WHERE ruta_id = ?", id))
        assertEquals(0L, contar("SELECT count(*) FROM reasignacion_ruta WHERE ruta_id = ?", id))
        // la programación del conductor y el conductor siguen existiendo
        assertEquals(1L, contar("SELECT count(*) FROM programacion WHERE id = ?", programacion))
    }

    @Test
    fun `CA-05_2 no elimina una ruta con recorridos activos`() {
        val id = registrarYObtenerId()
        insertarRecorrido(rutaCodigo = "R1")

        mockMvc.delete("$base/$id").andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") { value("No se puede eliminar: la ruta tiene recorridos activos.") }
        }
        mockMvc.get("$base/$id").andExpect { status { isOk() } }
    }

    // ---------------------------------------------------------------- Lista

    @Test
    fun `lista resumida cuenta recorridos activos y filtra por texto`() {
        registrarYObtenerId(cuerpo(alternos = listOf(alterno("A"))))
        registrarYObtenerId(cuerpo(codigo = "R2", nombre = "Bello Horizonte – Terminal"))
        insertarRecorrido(rutaCodigo = "R1")
        insertarRecorrido(rutaCodigo = "R1")
        insertarRecorrido(rutaCodigo = "R1", estado = "FINALIZADO")

        mockMvc.get(base).andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].codigo") { value("R1") }
            jsonPath("$[0].recorridosActivos") { value(2) }
            jsonPath("$[0].numAlternos") { value(1) }
            jsonPath("$[0].puntosRecorrido") { value(3) }
            jsonPath("$[0].recorridoPrincipal") { doesNotExist() }
            jsonPath("$[1].recorridosActivos") { value(0) }
        }
        mockMvc.get(base) { param("q", "bello") }.andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].codigo") { value("R2") }
        }
    }
}
