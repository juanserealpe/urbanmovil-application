package co.urbanmovil.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.domain.limites
import co.urbanmovil.app.ui.theme.UrbanColores
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.mapa_desactualizado

/** Estilo gratuito de OpenFreeMap: sin API key, sin registro, sin facturación. */
private val ESTILO_OPENFREEMAP = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty")

/**
 * Estilo de respaldo cuando el estilo remoto no carga (sin internet): solo un fondo liso.
 * Las líneas y marcadores se siguen dibujando encima.
 */
private val ESTILO_SIN_CONEXION = BaseStyle.Json(
    """{"version":8,"name":"UrbanMovil sin conexión","sources":{},""" +
        """"layers":[{"id":"fondo","type":"background","paint":{"background-color":"#E9ECEF"}}]}""",
)

/** Margen alrededor de lo encuadrado para que pines y buses no queden pegados al borde. */
private val MARGEN_ENCUADRE = DpPadding(48.dp, 48.dp, 48.dp, 48.dp)

/**
 * Mapa de la app. Es el ÚNICO lugar que conoce la librería de mapas (MapLibre Compose); si hay que
 * cambiarla, solo cambia este archivo.
 *
 * @param centro y [zoom] fijan la cámara INICIAL. Para moverla después, usar [camara].
 * @param lineas se dibujan en orden: la última queda encima.
 * @param descripcion `contentDescription` del mapa, p. ej. «Mapa de la ruta R1».
 * @param onTocarMapa toque sobre el mapa (fuera de marcadores). Se usa en la Fase 3.
 */
@Composable
fun MapaUrbano(
    centro: Punto,
    zoom: Double,
    lineas: List<LineaMapa>,
    marcadores: List<MarcadorMapa>,
    descripcion: String,
    modifier: Modifier = Modifier,
    camara: CamaraMapa = rememberCamaraMapa(),
    onTocarMapa: ((Punto) -> Unit)? = null,
) {
    // Al volver a entrar a la pantalla se reintenta el estilo remoto.
    var sinConexion by remember { mutableStateOf(false) }

    val estado = rememberMapState(
        baseStyle = if (sinConexion) ESTILO_SIN_CONEXION else ESTILO_OPENFREEMAP,
        initialCameraPosition = CameraPosition(center = centro.aPosition(), zoom = zoom),
    ) {
        lineas.forEach { linea -> key(linea.id) { CapaLinea(linea) } }
    }

    LaunchedEffect(estado) {
        snapshotFlow { estado.style.loadState }.collect { carga ->
            if (carga is StyleLoadState.Failed) sinConexion = true
        }
    }
    EjecutarOrdenesDeCamara(estado, camara)

    val alTocar by rememberUpdatedState(onTocarMapa)
    val interacciones = remember {
        MapInteractions {
            callbacks {
                click {
                    onEvent { evento ->
                        val callback = alTocar
                        val posicion = evento.position
                        if (callback != null && posicion != null) {
                            callback(Punto(lat = posicion.latitude, lng = posicion.longitude))
                            ClickResult.Consume
                        } else {
                            ClickResult.Pass
                        }
                    }
                }
            }
        }
    }

    val etiquetaDesactualizado = stringResource(Res.string.mapa_desactualizado)
    Box(modifier.background(UrbanColores.Fondo).semantics { contentDescription = descripcion }) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = estado,
            interactions = interacciones,
            overlay = {
                marcadores.forEach { marcador ->
                    key(marcador.id) {
                        MarcadorVista(
                            marcador = marcador,
                            etiquetaDesactualizado = etiquetaDesactualizado,
                            modifier = Modifier.placedAt(marcador.punto.aPosition(), marcador.tipo.anclaje()),
                        )
                    }
                }
                // Logo de MapLibre y botón de atribución (© OpenMapTiles © OpenStreetMap), exigidos por el estilo.
                include(MapOverlay.AttributionOnly)
            },
        )
    }
}

@Composable
@MaplibreComposable
private fun CapaLinea(linea: LineaMapa) {
    if (linea.puntos.size < 2) return
    val fuente = rememberGeoJsonSource(GeoJsonData.Features(LineString(linea.puntos.map { it.aPosition() })))
    LineLayer(
        id = "linea-${linea.id}",
        source = fuente,
        color = const(linea.color),
        width = const(linea.ancho),
        opacity = const(linea.opacidad),
        // Punteado en múltiplos del ancho de la línea: 2 de trazo, 1.5 de hueco.
        dasharray = if (linea.punteada) const(listOf<Number>(2, 1.5)) else null,
        cap = const(if (linea.punteada) LineCap.Butt else LineCap.Round),
        join = const(LineJoin.Round),
    )
}

@Composable
private fun EjecutarOrdenesDeCamara(estado: MapState, camara: CamaraMapa) {
    LaunchedEffect(estado, camara) {
        var movimiento: Job? = null
        for (orden in camara.ordenes) {
            // Una orden nueva reemplaza a la anterior. Si el usuario toca el mapa a mitad de una
            // animación, la librería cancela ese hijo y el bucle sigue escuchando.
            movimiento?.cancel()
            movimiento = launch {
                when (orden) {
                    is OrdenCamara.Centrar ->
                        estado.animateCamera(CameraUpdate(center = orden.punto.aPosition(), zoom = orden.zoom))

                    is OrdenCamara.Encuadrar -> {
                        val l = limites(orden.puntos) ?: return@launch
                        val caja = BoundingBox(west = l.oeste, south = l.sur, east = l.este, north = l.norte)
                        val ajuste = CameraFit(fitPadding = MARGEN_ENCUADRE)
                        if (orden.animar) estado.animateCameraToBounds(caja, ajuste) else estado.fitCameraToBounds(caja, ajuste)
                    }
                }
            }
        }
    }
}

private fun Punto.aPosition() = Position(longitude = lng, latitude = lat)
