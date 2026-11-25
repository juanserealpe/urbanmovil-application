package co.urbanmovil.app.ui.viaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.urbanmovil.app.data.TransitRepository
import co.urbanmovil.app.domain.busesVisibles
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.ui.rutas.BusUi
import co.urbanmovil.app.ui.rutas.ControlRelojDebug
import co.urbanmovil.app.ui.rutas.aBusUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CuandoPasaUi(
    val punto: Punto,
    /** P = Mi ubicación (estado inicial). */
    val enMiUbicacion: Boolean,
    /** Modo «Punto en el mapa»: tocar el mapa mueve P. */
    val modoMapa: Boolean,
    val rutas: List<Ruta>,
    val buses: List<BusUi>,
    val tarjetas: List<TarjetaEta>,
    val miUbicacion: Lugar,
)

/** HU-20. Los tiempos se recalculan cada segundo con el flujo de buses. */
class CuandoPasaViewModel(private val repo: TransitRepository) : ViewModel() {

    val reloj = ControlRelojDebug(repo)

    private val miUbicacion = repo.miUbicacion()
    private val punto = MutableStateFlow(miUbicacion.punto)
    private val modoMapa = MutableStateFlow(false)

    val ui: StateFlow<CuandoPasaUi> =
        combine(repo.estadosBuses(), punto, modoMapa) { estados, p, modo -> construir(estados, p, modo) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                construir(repo.estadosEn(repo.ahoraMs()), punto.value, modoMapa.value),
            )

    private fun construir(estados: List<EstadoBus>, p: Punto, modo: Boolean): CuandoPasaUi {
        val rutas = repo.rutas()
        val porId = rutas.associateBy { it.id }
        val ahora = repo.ahoraMs()
        return CuandoPasaUi(
            punto = p,
            enMiUbicacion = p == miUbicacion.punto,
            modoMapa = modo,
            rutas = rutas,
            buses = busesVisibles(estados).map { it.aBusUi(porId[it.bus.rutaId], ahora) },
            tarjetas = construirTarjetasEta(p, rutas, estados, repo.lugares()),
            miUbicacion = miUbicacion,
        )
    }

    fun usarMiUbicacion() {
        punto.value = miUbicacion.punto
        modoMapa.value = false
    }

    fun activarPuntoEnMapa() {
        modoMapa.value = true
    }

    /** Solo mueve P en modo «Punto en el mapa». */
    fun tocarMapa(p: Punto) {
        if (modoMapa.value) punto.value = p
    }
}

/**
 * HU-21, HU-22 y HU-23. Lo comparten «Buscar viaje» y «Guía» (la guía lo toma de la entrada de la
 * pestaña en el back stack), así volver a la lista conserva la búsqueda (CA-23.2).
 */
class BuscarViajeViewModel(private val repo: TransitRepository) : ViewModel() {

    val reloj = ControlRelojDebug(repo)
    val lugares: List<Lugar> = repo.lugares()
    val miUbicacion: Lugar = repo.miUbicacion()
    val rutas: List<Ruta> = repo.rutas()

    /** Demo: Mi ubicación (barrio Valencia) → Universidad del Cauca – Sede Tulcán. */
    private val origen = MutableStateFlow<PuntoElegido>(PuntoElegido.MiUbicacion(miUbicacion.punto))
    private val destino = MutableStateFlow<PuntoElegido>(
        (lugares.firstOrNull { it.nombre.contains("Tulcán") } ?: lugares.firstOrNull())
            ?.let { PuntoElegido.DeLugar(it) }
            ?: PuntoElegido.MiUbicacion(miUbicacion.punto),
    )

    val ui: StateFlow<BuscarViajeUi> =
        combine(repo.estadosBuses(), origen, destino) { estados, o, d -> construir(estados, o, d) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                construir(repo.estadosEn(repo.ahoraMs()), origen.value, destino.value),
            )

    private fun construir(estados: List<EstadoBus>, o: PuntoElegido, d: PuntoElegido) =
        BuscarViajeUi(o, d, construirResultadoBusqueda(o, d, repo.rutas(), estados, lugares))

    fun elegirOrigen(punto: PuntoElegido) {
        origen.value = punto
    }

    fun elegirDestino(punto: PuntoElegido) {
        destino.value = punto
    }

    fun intercambiar() {
        val o = origen.value
        origen.value = destino.value
        destino.value = o
    }
}
