package co.urbanmovil.app.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import co.urbanmovil.app.data.model.Punto
import kotlinx.coroutines.channels.Channel

/*
 * Modelo del mapa independiente de la librería. Las pantallas SOLO conocen estos tipos y
 * [MapaUrbano]; cambiar MapLibre por otra cosa no las toca.
 */

/**
 * Polilínea a dibujar.
 * @param id identifica la GEOMETRÍA: dos líneas con el mismo id deben tener los mismos puntos.
 *   Color, ancho y opacidad sí pueden cambiar sin cambiar el id.
 * @param punteada `true` para caminos alternos (se distinguen por forma, no solo por color).
 */
@Immutable
data class LineaMapa(
    val id: String,
    val puntos: List<Punto>,
    val color: Color,
    val ancho: Dp,
    val punteada: Boolean = false,
    val opacidad: Float = 1f,
)

/** Qué se dibuja en un punto del mapa. */
@Immutable
sealed interface TipoMarcador {
    /** Bus en recorrido. Si [desactualizado], círculo blanco con borde gris punteado y etiqueta. */
    data class Bus(val colorRuta: Color, val desactualizado: Boolean) : TipoMarcador

    /** Pin con una letra: «I» inicio, «F» final; en la Fase 3 «A», «B», «D», «P». */
    data class Pin(val letra: String, val color: Color) : TipoMarcador

    data object MiUbicacion : TipoMarcador

    /** Círculo simple con borde blanco: punto del recorrido más cercano, origen de un viaje. */
    data class Punto(val color: Color, val grande: Boolean = false) : TipoMarcador
}

/**
 * @param descripcion texto para lectores de pantalla, p. ej. «Bus UM-104, ubicación reciente».
 * @param alTocar `null` si el marcador no es interactivo.
 */
@Immutable
data class MarcadorMapa(
    val id: String,
    val punto: Punto,
    val tipo: TipoMarcador,
    val descripcion: String,
    val alTocar: (() -> Unit)? = null,
)

/** Órdenes de cámara que las pantallas le dan a [MapaUrbano]. */
internal sealed interface OrdenCamara {
    data class Centrar(val punto: Punto, val zoom: Double?) : OrdenCamara
    data class Encuadrar(val puntos: List<Punto>, val animar: Boolean) : OrdenCamara
}

/**
 * Control de cámara desacoplado de la librería. Las órdenes que llegan antes de que el mapa
 * exista se guardan (solo la última) y se ejecutan en cuanto el mapa tiene tamaño.
 */
@Stable
class CamaraMapa {
    internal val ordenes = Channel<OrdenCamara>(Channel.CONFLATED)

    /** Mueve la cámara a [punto] (p. ej. «Centrar en mi ubicación»). */
    fun centrarEn(punto: Punto, zoom: Double? = null) {
        ordenes.trySend(OrdenCamara.Centrar(punto, zoom))
    }

    /** Ajusta la cámara para que se vean todos los [puntos] (p. ej. el recorrido de una ruta). */
    fun encuadrar(puntos: List<Punto>, animar: Boolean = true) {
        if (puntos.isNotEmpty()) ordenes.trySend(OrdenCamara.Encuadrar(puntos, animar))
    }
}

@Composable
fun rememberCamaraMapa(): CamaraMapa = remember { CamaraMapa() }
