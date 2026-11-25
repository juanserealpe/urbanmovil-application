package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Vigencia
import co.urbanmovil.app.data.remoto.UrbanMovilApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RemoteTransitRepositoryTest {

    private val inicio = 1_795_532_400_000L // 2026-11-24T10:00:00-05:00

    private val rutasJson = """
        [{"codigo":"R1","nombre":"Centro – Campanario","color":"#0057B8",
          "recorridoPrincipal":[{"lat":2.4419,"lng":-76.6066},{"lat":2.4440,"lng":-76.6050},{"lat":2.4462,"lng":-76.6031},
                                {"lat":2.4490,"lng":-76.6010},{"lat":2.4510,"lng":-76.5990}],
          "alternos":[{"nombre":"Desvío","puntos":[{"lat":2.44,"lng":-76.60},{"lat":2.45,"lng":-76.61}]}]}]
    """.trimIndent()

    /** El servidor responde a las 10:00:30: el recorrido lleva 30 s. */
    private fun busesJson(buses: String = """[{"recorridoId":7,"numeroInterno":"UM-104","rutaCodigo":"R1",
            "inicio":"2026-11-24T10:00:00-05:00","posicion":{"lat":2.4425,"lng":-76.6061}}]""") = """
        {"simulado":true,"generadoEn":"2026-11-24T10:00:30-05:00","velocidadKmh":18.0,"buses":$buses}
    """.trimIndent()

    private val lugaresJson = """
        {"ubicacionPasajero":{"nombre":"Mi ubicación","punto":{"lat":2.44,"lng":-76.613}},
         "lugares":[{"nombre":"Terminal de Transportes","punto":{"lat":2.445,"lng":-76.599}},
                    {"nombre":"Estadio Municipal","punto":{"lat":2.438,"lng":-76.61}}]}
    """.trimIndent()

    private class Servidor(var buses: String, val rutas: String, var falla: Boolean = false)

    private fun repositorio(servidor: Servidor, relojLocal: () -> Long): RemoteTransitRepository {
        val motor = MockEngine { solicitud ->
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                servidor.falla -> respondError(HttpStatusCode.ServiceUnavailable)
                solicitud.url.encodedPath == "/api/v1/public/rutas" -> respond(servidor.rutas, HttpStatusCode.OK, json)
                solicitud.url.encodedPath == "/api/v1/public/buses" -> respond(servidor.buses, HttpStatusCode.OK, json)
                solicitud.url.encodedPath == "/api/v1/public/mapa/lugares" -> respond(lugaresJson, HttpStatusCode.OK, json)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        return RemoteTransitRepository(UrbanMovilApi("http://servidor", UrbanMovilApi.clienteHttp(motor)), reloj = relojLocal, sincronizarEnSegundoPlano = false)
    }

    @Test
    fun cargar_traeRutasYBusesDelBackend() = runTest {
        val repo = repositorio(Servidor(busesJson(), rutasJson)) { inicio + 30_000 }
        repo.cargar()

        val ruta = repo.rutas().single()
        assertEquals("R1", ruta.id)
        assertEquals("Centro – Campanario", ruta.nombre)
        assertEquals(0xFF0057B8, ruta.colorHex)
        assertEquals(5, ruta.principal.size)
        assertEquals(1, ruta.alternos.size)
        assertTrue(ruta.minutosPorVuelta >= 1)
        assertEquals(ruta, repo.ruta("R1"))

        val bus = repo.buses().single()
        assertEquals("UM-104", bus.id)
        assertEquals("R1", bus.rutaId)
    }

    @Test
    fun cargar_traeLosLugaresDeLaCiudadYLaUbicacionDelPasajero() = runTest {
        val repo = repositorio(Servidor(busesJson(), rutasJson)) { inicio + 30_000 }
        repo.cargar()

        assertEquals(listOf("Terminal de Transportes", "Estadio Municipal"), repo.lugares().map { it.nombre })
        assertEquals(Punto(2.445, -76.599), repo.lugares().first().punto)
        assertEquals("Mi ubicación", repo.miUbicacion().nombre)
        assertEquals(Punto(2.44, -76.613), repo.miUbicacion().punto)
    }

    @Test
    fun laPosicionSeCalculaLocalmenteConLaMismaFormulaQueElBackend() = runTest {
        val repo = repositorio(Servidor(busesJson(), rutasJson)) { inicio + 30_000 }
        repo.cargar()

        // 60 s después de la respuesta: el recorrido lleva 90 s → valor del contrato compartido.
        val estado = repo.estadosEn(inicio + 90_000).single()
        val esperado = Punto(2.445065833931734, -76.60407950705896)
        assertTrue(abs(estado.posicion.lat - esperado.lat) < 1e-9 && abs(estado.posicion.lng - esperado.lng) < 1e-9, "${estado.posicion}")
        assertTrue(abs(estado.posicionEnRecorridoM - 450.0) < 1e-6)
    }

    @Test
    fun corrigeElDesfaseDelRelojDelTelefono() = runTest {
        // El teléfono va 2 minutos atrasado respecto al servidor.
        var local = inicio + 30_000 - 120_000
        val repo = repositorio(Servidor(busesJson(), rutasJson)) { local }
        repo.cargar()

        assertEquals(inicio + 30_000, repo.ahoraMs())
        local += 5_000
        assertEquals(inicio + 35_000, repo.ahoraMs())
    }

    @Test
    fun vigencia_reciente_yDesactualizadaSiNoSePudoSincronizarHaceMasDe60s() = runTest {
        val repo = repositorio(Servidor(busesJson(), rutasJson)) { inicio + 30_000 }
        repo.cargar()

        assertEquals(Vigencia.RECIENTE, repo.estadosEn(inicio + 40_000).single().vigencia)
        assertEquals(inicio + 30_000, repo.estadosEn(inicio + 40_000).single().horaUltimaUbicacion)
        assertEquals(Vigencia.DESACTUALIZADA, repo.estadosEn(inicio + 30_000 + 61_000).single().vigencia)
    }

    @Test
    fun sincronizar_quitaLosRecorridosFinalizados() = runTest {
        val servidor = Servidor(busesJson(), rutasJson)
        val repo = repositorio(servidor) { inicio + 30_000 }
        repo.cargar()
        assertEquals(1, repo.buses().size)

        servidor.buses = busesJson("[]")
        repo.sincronizar()
        assertTrue(repo.buses().isEmpty())
        assertTrue(repo.estadosEn(repo.ahoraMs()).isEmpty())
    }

    @Test
    fun siLaSincronizacionFalla_seConservanLosUltimosDatos() = runTest {
        val servidor = Servidor(busesJson(), rutasJson)
        val repo = repositorio(servidor) { inicio + 30_000 }
        repo.cargar()

        servidor.falla = true
        assertFailsWith<Exception> { repo.sincronizar() }
        assertEquals(1, repo.buses().size)
    }

    @Test
    fun cargar_fallaSiElServidorNoResponde() = runTest {
        val repo = repositorio(Servidor(busesJson(), rutasJson, falla = true)) { inicio }
        assertFailsWith<Exception> { repo.cargar() }
        assertTrue(repo.rutas().isEmpty())
    }

    @Test
    fun unBusDeUnaRutaDesconocidaSeIgnora() = runTest {
        val buses = """[{"recorridoId":9,"numeroInterno":"UM-999","rutaCodigo":"R9","inicio":"2026-11-24T10:00:00-05:00",
            "posicion":{"lat":2.44,"lng":-76.6}}]"""
        val repo = repositorio(Servidor(busesJson(buses), rutasJson)) { inicio + 30_000 }
        repo.cargar()
        assertTrue(repo.estadosEn(repo.ahoraMs()).isEmpty())
    }
}
