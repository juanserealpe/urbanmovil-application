package co.urbanmovil.api.reasignacion

import co.urbanmovil.api.soporte.OperacionTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import java.time.Duration
import kotlin.test.assertEquals

/** HU-25 · §4 de la Fase 3. */
class ReasignacionIntegracionTest : OperacionTest() {

    private var carlos = 0L
    private var laura = 0L
    private var r1 = 0L
    private var r2 = 0L
    private var r3 = 0L
    private var bus = 0L

    @BeforeEach
    fun escenario() {
        carlos = crearConductor("Carlos Chicangana", 3)
        laura = crearConductor("Laura Idrobo", 2)
        r1 = crearRuta("R1")
        r2 = crearRuta("R2")
        r3 = crearRuta("R3")
        bus = crearVehiculo("UM-104", "ABC104")
    }

    private fun programacionDe(conductorId: Long) = mockMvc.get("/api/v1/conductores/$conductorId/programacion")

    @Test
    fun `CA-25_1 reasignacion valida actualiza la programacion y queda registrada`() {
        programar(carlos, r1, r2).andExpect { status { isOk() } }

        reasignar(carlos, "  Diana reportó falla mecánica  ", asignar = listOf(r3), quitar = listOf(r1)).andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.fechaHora") { value(org.hamcrest.Matchers.startsWith("2026-11-24T10:00")) }
            jsonPath("$.administrador.id") { isNumber() }
            jsonPath("$.administrador.nombre") { value("Administrador") }
            jsonPath("$.conductor.id") { value(carlos) }
            jsonPath("$.motivo") { value("Diana reportó falla mecánica") }
            jsonPath("$.rutasAsignadas[0]") { value("R3") }
            jsonPath("$.rutasQuitadas[0]") { value("R1") }
            jsonPath("$.programacion.rutas.length()") { value(2) }
            jsonPath("$.programacion.rutas[0].codigo") { value("R2") }
            jsonPath("$.programacion.rutas[0].orden") { value(1) }
            jsonPath("$.programacion.rutas[1].codigo") { value("R3") }
            jsonPath("$.programacion.rutas[1].orden") { value(2) }
        }

        val filas = jdbc.queryForList("SELECT tipo FROM reasignacion_ruta ORDER BY tipo", String::class.java)
        assertEquals(listOf("ASIGNADA", "QUITADA"), filas)
    }

    @Test
    fun `CA-25_1 sin programacion de hoy la crea y agrega al final en el orden recibido`() {
        reasignar(carlos, "Cubre a Diana", asignar = listOf(r2, r1)).andExpect {
            status { isCreated() }
            jsonPath("$.programacion.rutas[0].codigo") { value("R2") }
            jsonPath("$.programacion.rutas[1].codigo") { value("R1") }
        }
        programacionDe(carlos).andExpect { jsonPath("$.rutas.length()") { value(2) } }
    }

    @Test
    fun `CA-25_2 motivo vacio responde 400 Escriba que paso`() {
        programar(carlos, r1).andExpect { status { isOk() } }

        listOf("", "   ", null).forEach { motivo ->
            reasignar(carlos, motivo, asignar = listOf(r2)).andExpect {
                status { isBadRequest() }
                jsonPath("$.codigo") { value("VALIDACION") }
                jsonPath("$.mensaje") { value("Escriba qué pasó") }
                jsonPath("$.campos[0].campo") { value("motivo") }
            }
        }
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM reasignacion", Long::class.javaObjectType))
    }

    @Test
    fun `CA-25_3 quitar una ruta en curso la deja en RETIRO_PENDIENTE`() {
        programar(carlos, r1, r2).andExpect { status { isOk() } }
        enRecorrido(carlos, r1, bus)

        reasignar(carlos, "Se le quita la R1", quitar = listOf(r1)).andExpect {
            status { isCreated() }
            jsonPath("$.programacion.rutas.length()") { value(2) }
            jsonPath("$.programacion.rutas[0].codigo") { value("R1") }
            jsonPath("$.programacion.rutas[0].estado") { value("RETIRO_PENDIENTE") }
        }
        assertEquals(
            true,
            jdbc.queryForObject(
                "SELECT retiro_pendiente FROM programacion_ruta WHERE ruta_id = ?", Boolean::class.javaObjectType, r1,
            ),
        )
    }

    @Test
    fun `D-11 de punta a punta quitar en curso, finalizar y la ruta sale de la programacion`() {
        programar(carlos, r1, r2).andExpect { status { isOk() } }
        val recorrido = enRecorrido(carlos, r1, bus)
        reasignar(carlos, "Se le quita la R1", quitar = listOf(r1)).andExpect { status { isCreated() } }

        // El conductor termina la R1 veinte minutos después
        reloj.avanzar(Duration.ofMinutes(20))
        finalizar(recorrido).andExpect {
            status { isOk() }
            jsonPath("$.estado") { value("FINALIZADO") }
            jsonPath("$.fin") { value(org.hamcrest.Matchers.startsWith("2026-11-24T10:20")) }
        }

        programacionDe(carlos).andExpect {
            jsonPath("$.rutas.length()") { value(1) }
            jsonPath("$.rutas[0].codigo") { value("R2") }
            jsonPath("$.rutas[0].orden") { value(1) }
            jsonPath("$.rutas[0].estado") { value("PENDIENTE") }
        }
        iniciarRecorrido(carlos, r1, bus).andExpect {
            status { isConflict() }
            jsonPath("$.mensaje") { value("La ruta no está en la programación de hoy del conductor") }
        }
    }

    @Test
    fun `CA-25_4 no cambia la programacion de los demas conductores`() {
        programar(carlos, r1).andExpect { status { isOk() } }
        programar(laura, r1, r2).andExpect { status { isOk() } }

        reasignar(carlos, "Cubre la R2", asignar = listOf(r2), quitar = listOf(r1)).andExpect { status { isCreated() } }

        programacionDe(laura).andExpect {
            jsonPath("$.rutas.length()") { value(2) }
            jsonPath("$.rutas[0].codigo") { value("R1") }
            jsonPath("$.rutas[1].codigo") { value("R2") }
        }
    }

    @Test
    fun `CA-25_5 sin rutas para asignar ni quitar responde 400`() {
        reasignar(carlos, "Algo pasó").andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("Indique al menos una ruta para asignar o quitar") }
        }
    }

    @Test
    fun `asignar una ruta que ya esta en la programacion responde 409`() {
        programar(carlos, r1).andExpect { status { isOk() } }

        reasignar(carlos, "Repetida", asignar = listOf(r1)).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") { value("La ruta R1 ya está en la programación") }
        }
    }

    @Test
    fun `quitar una ruta que no esta en la programacion responde 409`() {
        programar(carlos, r1).andExpect { status { isOk() } }

        reasignar(carlos, "No la tiene", quitar = listOf(r2)).andExpect {
            status { isConflict() }
            jsonPath("$.mensaje") { value("La ruta R2 no está en la programación") }
        }
    }

    @Test
    fun `la misma ruta en ambas listas responde 400`() {
        programar(carlos, r1).andExpect { status { isOk() } }

        reasignar(carlos, "Confuso", asignar = listOf(r2), quitar = listOf(r2)).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
        }
    }

    @Test
    fun `una ruta inexistente responde 404 y no se guarda nada`() {
        programar(carlos, r1).andExpect { status { isOk() } }

        reasignar(carlos, "Ruta fantasma", asignar = listOf(9999)).andExpect { status { isNotFound() } }
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM reasignacion", Long::class.javaObjectType))
    }

    @Test
    fun `historial mas reciente primero con filtros por conductor y fecha`() {
        programar(carlos, r1).andExpect { status { isOk() } }
        programar(laura, r1).andExpect { status { isOk() } }
        reasignar(carlos, "Primera", asignar = listOf(r2)).andExpect { status { isCreated() } }
        reloj.avanzar(Duration.ofMinutes(5))
        reasignar(laura, "Segunda", asignar = listOf(r3)).andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/reasignaciones").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].motivo") { value("Segunda") }
            jsonPath("$[0].programacion") { value(null) }
            jsonPath("$[1].motivo") { value("Primera") }
        }
        mockMvc.get("/api/v1/reasignaciones") { param("conductorId", carlos.toString()) }.andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].conductor.nombre") { value("Carlos Chicangana") }
        }
        mockMvc.get("/api/v1/reasignaciones") { param("fecha", "2026-11-24") }.andExpect { jsonPath("$.length()") { value(2) } }
        mockMvc.get("/api/v1/reasignaciones") { param("fecha", "2026-11-23") }.andExpect { jsonPath("$.length()") { value(0) } }
    }
}
