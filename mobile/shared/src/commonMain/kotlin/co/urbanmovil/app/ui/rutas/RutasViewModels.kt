package co.urbanmovil.app.ui.rutas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.urbanmovil.app.data.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val MS_POR_MIN = 60_000L

/**
 * Control oculto para adelantar el reloj simulado (Fase 2, §5.2). Las pantallas solo lo muestran
 * cuando `App(esDebug = true)`.
 */
class ControlRelojDebug(private val repo: TransitRepository) {
    val ajusteMs: StateFlow<Long> = repo.ajusteRelojMs

    fun adelantar(minutos: Int = MINUTOS_POR_TOQUE) = repo.ajustarReloj(repo.ajusteRelojMs.value + minutos * MS_POR_MIN)

    fun reiniciar() = repo.ajustarReloj(0)

    companion object {
        const val MINUTOS_POR_TOQUE = 3
    }
}

/** Pestaña Rutas (HU-18). La selección sobrevive a ir al detalle y volver. */
class RutasViewModel(private val repo: TransitRepository) : ViewModel() {

    val reloj = ControlRelojDebug(repo)

    private val seleccionadaId = MutableStateFlow(repo.rutas().firstOrNull()?.id)
    private val busSeleccionadoId = MutableStateFlow<String?>(null)

    val ui: StateFlow<RutasUi> =
        combine(repo.estadosBuses(), seleccionadaId, busSeleccionadoId) { estados, ruta, bus ->
            construirRutasUi(repo.rutas(), estados, repo.ahoraMs(), ruta, bus, repo.miUbicacion())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = construirRutasUi(
                repo.rutas(), repo.estadosEn(repo.ahoraMs()), repo.ahoraMs(),
                seleccionadaId.value, null, repo.miUbicacion(),
            ),
        )

    fun seleccionarRuta(rutaId: String) {
        seleccionadaId.value = rutaId
    }

    fun tocarBus(busId: String?) {
        busSeleccionadoId.value = busId
    }
}

/** Detalle de ruta (HU-18, HU-19, HU-24). Se actualiza cada segundo con el flujo del repositorio. */
class DetalleRutaViewModel(private val repo: TransitRepository, rutaId: String) : ViewModel() {

    val reloj = ControlRelojDebug(repo)

    private val ruta = repo.ruta(rutaId)
    private val busSeleccionadoId = MutableStateFlow<String?>(null)

    val ui: StateFlow<DetalleRutaUi> =
        combine(repo.estadosBuses(), busSeleccionadoId) { estados, bus ->
            construirDetalleRutaUi(ruta, estados, repo.ahoraMs(), bus)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = construirDetalleRutaUi(ruta, repo.estadosEn(repo.ahoraMs()), repo.ahoraMs(), null),
        )

    fun tocarBus(busId: String?) {
        busSeleccionadoId.value = busId
    }
}
