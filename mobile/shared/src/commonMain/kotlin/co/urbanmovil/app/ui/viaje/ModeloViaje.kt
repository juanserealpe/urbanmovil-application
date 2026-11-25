package co.urbanmovil.app.ui.viaje

import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.domain.OpcionViaje
import co.urbanmovil.app.domain.ProyeccionEnRecorrido
import co.urbanmovil.app.domain.ResultadoBusqueda
import co.urbanmovil.app.domain.ResultadoEta
import co.urbanmovil.app.domain.buscarViajes
import co.urbanmovil.app.domain.calcularEta
import co.urbanmovil.app.domain.contarBusesRecientes
import co.urbanmovil.app.domain.lugarMasCercano
import co.urbanmovil.app.domain.puntoMasCercano
import kotlin.math.roundToInt

/*
 * Capa de presentación de la Fase 3. Toda la lógica de tiempos y búsqueda viene de `domain/`
 * (calcularEta, buscarViajes); aquí solo se ordena, se nombra y se redondea para mostrar.
 */

// ------------------------------------------------------------------ ¿Cuándo pasa? (HU-20)

sealed interface EstadoEtaUi {
    /** «~N min» + «Pasa por [lugar] · a X m». [lugar] es `null` si no hay un nombre quemado cerca. */
    data class Llega(val minutos: Int, val lugar: String?, val distanciaM: Int) : EstadoEtaUi

    /** [busesRecientes] > 0 → ya pasaron; 0 → no hay ubicación reciente. */
    data class SinBus(val busesRecientes: Int) : EstadoEtaUi

    data object Lejos : EstadoEtaUi
}

/**
 * @param proyeccion punto del recorrido más cercano, solo si está a ≤ 500 m (para el círculo y la
 *   línea punteada del mapa). `null` cuando la ruta está lejos.
 */
data class TarjetaEta(val ruta: Ruta, val estado: EstadoEtaUi, val proyeccion: ProyeccionEnRecorrido?)

/**
 * Una tarjeta por ruta, ordenadas: primero las que tienen tiempo (de menor a mayor), luego
 * «Sin bus próximo», luego «Lejos de la ruta». Dentro de cada grupo se respeta el orden de [rutas].
 */
fun construirTarjetasEta(punto: Punto, rutas: List<Ruta>, estados: List<EstadoBus>, lugares: List<Lugar>): List<TarjetaEta> =
    rutas.map { ruta ->
        when (val eta = calcularEta(punto, ruta, estados)) {
            is ResultadoEta.Llega -> TarjetaEta(
                ruta = ruta,
                estado = EstadoEtaUi.Llega(
                    minutos = eta.minutos,
                    lugar = lugarMasCercano(eta.puntoDelRecorrido.punto, lugares)?.nombre,
                    distanciaM = eta.puntoDelRecorrido.distanciaM.roundToInt(),
                ),
                proyeccion = eta.puntoDelRecorrido,
            )
            ResultadoEta.SinBusProximo -> TarjetaEta(
                ruta = ruta,
                estado = EstadoEtaUi.SinBus(contarBusesRecientes(estados, ruta.id)),
                // calcularEta ya verificó que está a ≤ 500 m; se vuelve a proyectar solo para dibujarlo.
                proyeccion = puntoMasCercano(punto, ruta.principal),
            )
            ResultadoEta.LejosDeLaRuta -> TarjetaEta(ruta, EstadoEtaUi.Lejos, proyeccion = null)
        }
    }.sortedWith(compareBy({ it.estado.grupo() }, { (it.estado as? EstadoEtaUi.Llega)?.minutos ?: 0 }))

private fun EstadoEtaUi.grupo(): Int = when (this) {
    is EstadoEtaUi.Llega -> 0
    is EstadoEtaUi.SinBus -> 1
    EstadoEtaUi.Lejos -> 2
}

// ------------------------------------------------------------------ Buscar viaje (HU-21, HU-22)

/** Origen o destino elegido. El nombre visible lo pone la UI (strings). */
sealed interface PuntoElegido {
    val punto: Punto

    data class MiUbicacion(override val punto: Punto) : PuntoElegido
    data class DeLugar(val lugar: Lugar) : PuntoElegido {
        override val punto: Punto get() = lugar.punto
    }
    data class EnMapa(override val punto: Punto) : PuntoElegido
}

data class OpcionViajeUi(
    val opcion: OpcionViaje,
    val caminataAbordajeM: Int,
    val caminataDescensoM: Int,
    /** `null` = «Sin bus próximo». El tiempo de trayecto se conserva igual (CA-22.3). */
    val etaMin: Int?,
    val trayectoMin: Int,
    val lugarAbordaje: String?,
    val lugarDescenso: String?,
    /** Solo la primera de la lista (CA-21.2). */
    val menosCaminata: Boolean,
) {
    val id: String get() = opcion.id
    val ruta: Ruta get() = opcion.ruta
}

sealed interface ResultadoBusquedaUi {
    data object PuntosIguales : ResultadoBusquedaUi

    /** Vacía → «No hay rutas que conecten estos puntos» (CA-21.4). */
    data class Opciones(val opciones: List<OpcionViajeUi>) : ResultadoBusquedaUi
}

/** El orden es el de [buscarViajes] (menor caminata); los tiempos NO lo cambian (DP-15, CA-22.4). */
fun construirResultadoBusqueda(
    origen: PuntoElegido,
    destino: PuntoElegido,
    rutas: List<Ruta>,
    estados: List<EstadoBus>,
    lugares: List<Lugar>,
): ResultadoBusquedaUi = when (val resultado = buscarViajes(origen.punto, destino.punto, rutas, estados)) {
    ResultadoBusqueda.PuntosIguales -> ResultadoBusquedaUi.PuntosIguales
    is ResultadoBusqueda.Opciones -> ResultadoBusquedaUi.Opciones(
        resultado.opciones.mapIndexed { i, opcion ->
            OpcionViajeUi(
                opcion = opcion,
                caminataAbordajeM = opcion.caminataAbordajeM.roundToInt(),
                caminataDescensoM = opcion.caminataDescensoM.roundToInt(),
                etaMin = (opcion.etaAbordaje as? ResultadoEta.Llega)?.minutos,
                trayectoMin = opcion.minutosTrayecto,
                lugarAbordaje = lugarMasCercano(opcion.abordaje.punto, lugares)?.nombre,
                lugarDescenso = lugarMasCercano(opcion.descenso.punto, lugares)?.nombre,
                menosCaminata = i == 0,
            )
        },
    )
}

data class BuscarViajeUi(
    val origen: PuntoElegido,
    val destino: PuntoElegido,
    val resultado: ResultadoBusquedaUi,
)
