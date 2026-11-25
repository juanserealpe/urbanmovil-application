package co.urbanmovil.api.programacion

import co.urbanmovil.api.soporte.OperacionTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals

/** HU-09 · §3 de la Fase 3, y §7 (rutasHoy / conductoresHoy). */
class ProgramacionIntegracionTest : OperacionTest() {

    private var conductor = 0L
    private var r1 = 0L
    private var r2 = 0L
    private var r3 = 0L
    private var bus = 0L

    @BeforeEach
    fun escenario() {
        conductor = crearConductor("Andrés Muñoz", 1)
        r1 = crearRuta("R1")
        r2 = crearRuta("R2")
        r3 = crearRuta("R3")
        bus = crearVehiculo("UM-104", "ABC104")
    }

    private fun programacion(fecha: String? = null) =
        mockMvc.get("/api/v1/conductores/$conductor/programacion") { fecha?.let { param("fecha", it) } }

    @Test
    fun `sin programacion devuelve rutas vacia con la fecha de hoy`() {
        programacion().andExpect {
            status { isOk() }
            jsonPath("$.conductorId") { value(conductor) }
            jsonPath("$.fecha") { value("2026-11-24") }
            jsonPath("$.rutas") { isEmpty() }
        }
    }

    @Test
    fun `CA-09_1 asigna R2 y R1 y las devuelve en ese orden`() {
        programar(conductor, r2, r1).andExpect {
            status { isOk() }
            jsonPath("$.rutas.length()") { value(2) }
            jsonPath("$.rutas[0].codigo") { value("R2") }
            jsonPath("$.rutas[0].orden") { value(1) }
            jsonPath("$.rutas[1].codigo") { value("R1") }
            jsonPath("$.rutas[1].orden") { value(2) }
            jsonPath("$.rutas[0].estado") { value("PENDIENTE") }
        }
        programacion().andExpect {
            jsonPath("$.rutas[0].rutaId") { value(r2) }
            jsonPath("$.rutas[0].nombre") { value("Ruta R2") }
            jsonPath("$.rutas[0].color") { isString() }
            jsonPath("$.rutas[1].rutaId") { value(r1) }
        }
    }

    @Test
    fun `CA-09_2 un PUT sin una ruta sin recorrido activo la quita`() {
        programar(conductor, r1, r2).andExpect { status { isOk() } }

        programar(conductor, r2).andExpect {
            status { isOk() }
            jsonPath("$.rutas.length()") { value(1) }
            jsonPath("$.rutas[0].codigo") { value("R2") }
            jsonPath("$.rutas[0].orden") { value(1) }
        }
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM programacion_ruta", Long::class.javaObjectType))
    }

    @Test
    fun `CA-09_3 no retira la ruta que el conductor esta recorriendo`() {
        programar(conductor, r1, r2).andExpect { status { isOk() } }
        enRecorrido(conductor, r1, bus)

        programar(conductor, r2).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") {
                value("No se puede retirar la ruta R1: el conductor la está recorriendo. Se podrá retirar cuando termine.")
            }
        }
        programacion().andExpect {
            jsonPath("$.rutas.length()") { value(2) }
            jsonPath("$.rutas[0].codigo") { value("R1") }
            jsonPath("$.rutas[0].estado") { value("EN_RECORRIDO") }
            jsonPath("$.rutas[1].estado") { value("PENDIENTE") }
        }
    }

    @Test
    fun `CA-09_3 si la ruta en curso sigue en la lista se puede reordenar`() {
        programar(conductor, r1, r2).andExpect { status { isOk() } }
        enRecorrido(conductor, r1, bus)

        programar(conductor, r2, r1).andExpect {
            status { isOk() }
            jsonPath("$.rutas[1].codigo") { value("R1") }
            jsonPath("$.rutas[1].estado") { value("EN_RECORRIDO") }
        }
    }

    @Test
    fun `CA-09_4 una ruta repetida responde 400 y no crea duplicados`() {
        programar(conductor, r1, r2, r1).andExpect {
            status { isBadRequest() }
            jsonPath("$.codigo") { value("VALIDACION") }
            jsonPath("$.mensaje") { value("La ruta R1 está repetida en la programación") }
            jsonPath("$.campos[0].campo") { value("rutas") }
        }
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM programacion_ruta", Long::class.javaObjectType))
    }

    @Test
    fun `CA-09_5 un PUT con otro orden deja el nuevo orden`() {
        programar(conductor, r1, r2, r3).andExpect { status { isOk() } }

        programar(conductor, r3, r1, r2).andExpect {
            status { isOk() }
            jsonPath("$.rutas[0].codigo") { value("R3") }
            jsonPath("$.rutas[1].codigo") { value("R1") }
            jsonPath("$.rutas[2].codigo") { value("R2") }
            jsonPath("$.rutas[2].orden") { value(3) }
        }
    }

    @Test
    fun `una ruta que no existe responde 404`() {
        programar(conductor, r1, 9999).andExpect {
            status { isNotFound() }
            jsonPath("$.codigo") { value("NO_ENCONTRADO") }
        }
    }

    @Test
    fun `conductor que no existe responde 404`() {
        programar(9999, r1).andExpect { status { isNotFound() } }
    }

    @Test
    fun `una fecha pasada responde 409 y una futura se permite`() {
        putJson("/api/v1/conductores/$conductor/programacion?fecha=2026-11-23", json("rutas" to listOf(r1))).andExpect {
            status { isConflict() }
            jsonPath("$.codigo") { value("REGLA_NEGOCIO") }
            jsonPath("$.mensaje") { value("No se puede modificar la programación de un día pasado") }
        }

        putJson("/api/v1/conductores/$conductor/programacion?fecha=2026-11-25", json("rutas" to listOf(r2))).andExpect {
            status { isOk() }
            jsonPath("$.fecha") { value("2026-11-25") }
        }
        programacion("2026-11-25").andExpect { jsonPath("$.rutas[0].codigo") { value("R2") } }
        programacion().andExpect { jsonPath("$.rutas") { isEmpty() } } // hoy sigue vacío (V-5)
    }

    @Test
    fun `el PUT conserva retiro pendiente de las rutas que siguen`() {
        programar(conductor, r1, r2).andExpect { status { isOk() } }
        enRecorrido(conductor, r1, bus)
        reasignar(conductor, "Se le quita la R1", quitar = listOf(r1)).andExpect { status { isCreated() } }

        programar(conductor, r1, r2, r3).andExpect {
            status { isOk() }
            jsonPath("$.rutas[0].estado") { value("RETIRO_PENDIENTE") }
            jsonPath("$.rutas[2].codigo") { value("R3") }
        }
    }

    @Test
    fun `sin lista de rutas responde 400`() {
        putJson("/api/v1/conductores/$conductor/programacion", "{}").andExpect {
            status { isBadRequest() }
            jsonPath("$.campos[0].campo") { value("rutas") }
        }
    }

    @Test
    fun `resumen del dia lista a todos los conductores`() {
        val otro = crearConductor("Laura Idrobo", 2)
        programar(conductor, r1, r2).andExpect { status { isOk() } }
        val recorrido = enRecorrido(conductor, r1, bus)

        mockMvc.get("/api/v1/programaciones").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].conductorId") { value(conductor) }
            jsonPath("$[0].nombre") { value("Andrés Muñoz") }
            jsonPath("$[0].disponible") { value(true) }
            jsonPath("$[0].totalRutas") { value(2) }
            jsonPath("$[0].enRecorrido.recorridoId") { value(recorrido) }
            jsonPath("$[0].enRecorrido.rutaCodigo") { value("R1") }
            jsonPath("$[1].conductorId") { value(otro) }
            jsonPath("$[1].totalRutas") { value(0) }
            jsonPath("$[1].enRecorrido") { value(null) }
        }
    }

    // ---------------------------------------------------------------- §7

    @Test
    fun `rutasHoy en conductores y conductoresHoy en rutas`() {
        val laura = crearConductor("Laura Idrobo", 2)
        programar(conductor, r2, r1).andExpect { status { isOk() } }
        programar(laura, r1).andExpect { status { isOk() } }
        val recorrido = enRecorrido(conductor, r2, bus)

        mockMvc.get("/api/v1/conductores/$conductor").andExpect {
            jsonPath("$.rutasHoy.length()") { value(2) }
            jsonPath("$.rutasHoy[0]") { value("R2") }
            jsonPath("$.rutasHoy[1]") { value("R1") }
            jsonPath("$.enRecorrido.recorridoId") { value(recorrido) }
        }
        mockMvc.get("/api/v1/conductores").andExpect {
            jsonPath("$[?(@.id == $laura)].rutasHoy[0]") { value("R1") }
        }
        mockMvc.get("/api/v1/rutas").andExpect {
            jsonPath("$[0].codigo") { value("R1") }
            jsonPath("$[0].conductoresHoy.length()") { value(2) }
            jsonPath("$[0].conductoresHoy[0]") { value("Andrés Muñoz") }
            jsonPath("$[0].conductoresHoy[1]") { value("Laura Idrobo") }
            jsonPath("$[2].conductoresHoy") { isEmpty() }
        }
        mockMvc.get("/api/v1/rutas/$r2").andExpect {
            jsonPath("$.conductoresHoy[0]") { value("Andrés Muñoz") }
            jsonPath("$.recorridosActivos") { value(1) }
        }
    }

    @Test
    fun `rutasHoy solo cuenta la programacion de hoy`() {
        putJson("/api/v1/conductores/$conductor/programacion?fecha=2026-11-25", json("rutas" to listOf(r1))).andExpect {
            status { isOk() }
        }
        mockMvc.get("/api/v1/conductores/$conductor").andExpect { jsonPath("$.rutasHoy") { isEmpty() } }
    }
}
