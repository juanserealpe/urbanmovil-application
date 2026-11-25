package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Vigencia
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeRepositoriesTest {

    private val horaFija = 1_767_268_800_000L // 2026-01-01T12:00Z

    @Test
    fun fakeData_cumpleLoPedido() {
        assertEquals(3, FakeData.rutas.size)
        assertEquals(5, FakeData.buses.size)
        assertTrue(FakeData.lugares.size >= 6)
        FakeData.rutas.forEach { assertTrue(it.principal.size in 15..40, "${it.id}: ${it.principal.size} puntos") }
        assertEquals(1, FakeData.R1.alternos.size)
        assertEquals(3, FakeData.buses.count { it.ocupacion != null })
        assertTrue(FakeData.buses.any { (it.ocupacionReportadaHaceMin ?: 0) > FakeData.OCUPACION_VIGENTE_MIN })
    }

    @Test
    fun flujoDeBuses_emiteUnEstadoPorBus() = runTest {
        val repo = FakeTransitRepository(reloj = { horaFija })
        val estados = repo.estadosBuses().first()
        assertEquals(FakeData.buses.map { it.id }, estados.map { it.bus.id })
    }

    @Test
    fun ajusteDeReloj_adelantaLaSimulacion() {
        val repo = FakeTransitRepository(reloj = { horaFija })
        val antes = repo.estadosEn(repo.ahoraMs())
        repo.ajustarReloj(3 * 60_000L)
        assertEquals(horaFija + 3 * 60_000L, repo.ahoraMs())
        assertNotEquals(antes, repo.estadosEn(repo.ahoraMs()))
    }

    @Test
    fun r3_quedaSinBusesActivosParteDelTiempo() {
        val repo = FakeTransitRepository(reloj = { horaFija })
        val vuelta = FakeData.R3.minutosPorVuelta
        val ocultoAlgunaVez = (0 until vuelta).any { minuto ->
            repo.estadosEn(horaFija + minuto * 60_000L)
                .single { it.bus.rutaId == "R3" }.vigencia == Vigencia.OCULTA
        }
        assertTrue(ocultoAlgunaVez)
    }

    @Test
    fun login_credencialesDemo_iniciaSesion() {
        val auth = FakeAuthRepository()
        assertTrue(auth.iniciarSesion(" Pasajero@Demo.com ", "123456"))
        assertEquals(FakeData.USUARIO_DEMO, auth.sesion.value)
    }

    @Test
    fun login_claveIncorrecta_noIniciaSesion() {
        val auth = FakeAuthRepository()
        assertFalse(auth.iniciarSesion("pasajero@demo.com", "000000"))
        assertNull(auth.sesion.value)
    }

    @Test
    fun crearCuenta_entraComoDemo_yCerrarSesionLaBorra() {
        val auth = FakeAuthRepository()
        auth.entrarComoDemo()
        assertEquals(FakeData.USUARIO_DEMO, auth.sesion.value)
        auth.cerrarSesion()
        assertNull(auth.sesion.value)
    }
}
