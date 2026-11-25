package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Ruta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
private fun relojDelSistema(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Implementación con datos quemados ([FakeData]) y buses simulados por hora ([BusSimulator]).
 * @param reloj fuente de la hora; inyectable para pruebas.
 */
class FakeTransitRepository(
    private val reloj: () -> Long = ::relojDelSistema,
    private val intervalo: Duration = 1.seconds,
) : TransitRepository {

    private val _ajusteRelojMs = MutableStateFlow(0L)
    override val ajusteRelojMs: StateFlow<Long> = _ajusteRelojMs.asStateFlow()

    override fun rutas(): List<Ruta> = FakeData.rutas
    override fun ruta(id: String): Ruta? = FakeData.rutas.firstOrNull { it.id == id }
    override fun buses(): List<Bus> = FakeData.buses
    override fun lugares(): List<Lugar> = FakeData.lugares
    override fun miUbicacion(): Lugar = FakeData.MI_UBICACION

    override fun ajustarReloj(ajusteMs: Long) {
        _ajusteRelojMs.value = ajusteMs
    }

    override fun ahoraMs(): Long = reloj() + _ajusteRelojMs.value

    override fun estadosEn(ahoraMs: Long): List<EstadoBus> =
        BusSimulator.estados(FakeData.buses, FakeData.rutas, ahoraMs)

    override fun estadosBuses(): Flow<List<EstadoBus>> = flow {
        while (true) {
            emit(estadosEn(ahoraMs()))
            delay(intervalo)
        }
    }.flowOn(Dispatchers.Default)
}
