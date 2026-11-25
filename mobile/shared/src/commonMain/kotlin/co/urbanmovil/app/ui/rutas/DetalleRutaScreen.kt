package co.urbanmovil.app.ui.rutas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.urbanmovil.app.LocalAppContainer
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.ui.map.MapaUrbano
import co.urbanmovil.app.ui.map.MarcadorMapa
import co.urbanmovil.app.ui.map.TipoMarcador
import co.urbanmovil.app.ui.map.rememberCamaraMapa
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.detalle_buses_titulo
import urbanmovil.shared.generated.resources.detalle_ruta_no_encontrada
import urbanmovil.shared.generated.resources.detalle_sin_buses
import urbanmovil.shared.generated.resources.detalle_subtitulo_alternos
import urbanmovil.shared.generated.resources.detalle_subtitulo_sin_alternos
import urbanmovil.shared.generated.resources.detalle_titulo
import urbanmovil.shared.generated.resources.leyenda_alterno
import urbanmovil.shared.generated.resources.leyenda_principal
import urbanmovil.shared.generated.resources.mapa_pin_final
import urbanmovil.shared.generated.resources.mapa_pin_inicio
import urbanmovil.shared.generated.resources.mapa_ruta_descripcion

private const val ZOOM_RUTA = 14.0

/** Detalle de ruta y buses (mockup «03 · Detalle de ruta y buses»). */
@Composable
fun DetalleRutaScreen(rutaId: String, alVolver: () -> Unit) {
    val repo = LocalAppContainer.current.transitRepository
    val viewModel = viewModel(key = "detalle-$rutaId") { DetalleRutaViewModel(repo, rutaId) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    DetalleRutaContenido(ui, viewModel.reloj, alVolver, alTocarBus = viewModel::tocarBus)
}

@Composable
internal fun DetalleRutaContenido(
    ui: DetalleRutaUi,
    reloj: ControlRelojDebug,
    alVolver: () -> Unit,
    alTocarBus: (String?) -> Unit,
) {
    val ruta = ui.ruta
    if (ruta == null) {
        Column(Modifier.fillMaxSize()) {
            EncabezadoPantalla(titulo = "", subtitulo = "", reloj = reloj, alVolver = alVolver)
            MensajeCentrado(stringResource(Res.string.detalle_ruta_no_encontrada), Modifier.fillMaxSize())
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(
            titulo = stringResource(Res.string.detalle_titulo, ruta.id, ruta.nombre),
            subtitulo = subtituloAlternos(ruta.alternos.size),
            reloj = reloj,
            alVolver = alVolver,
        )
        MapaDeRuta(ruta, ui, alTocarBus, Modifier.fillMaxWidth().weight(1f))
        Leyenda(ruta)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ListaBuses(ui.buses, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun subtituloAlternos(n: Int): String =
    if (n == 0) {
        stringResource(Res.string.detalle_subtitulo_sin_alternos)
    } else {
        pluralStringResource(Res.plurals.detalle_subtitulo_alternos, n, n)
    }

@Composable
private fun MapaDeRuta(ruta: Ruta, ui: DetalleRutaUi, alTocarBus: (String?) -> Unit, modifier: Modifier) {
    val camara = rememberCamaraMapa()
    LaunchedEffect(ruta.id) { camara.encuadrar(ruta.principal + ruta.alternos.flatten(), animar = false) }

    val lineas = remember(ruta) { lineasDeRuta(ruta) }
    val marcadores = pinesDeRuta(ruta) + marcadoresDeBuses(ui.buses) { alTocarBus(it.id) }

    Box(modifier) {
        MapaUrbano(
            centro = ruta.principal.first(),
            zoom = ZOOM_RUTA,
            lineas = lineas,
            marcadores = marcadores,
            descripcion = stringResource(Res.string.mapa_ruta_descripcion, ruta.id),
            camara = camara,
            modifier = Modifier.fillMaxSize(),
        )
        ui.busSeleccionado?.let { bus ->
            TarjetaBus(bus, alCerrar = { alTocarBus(null) }, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp))
        }
    }
}

/** Pin verde «I» en el inicio y oscuro «F» en el final del recorrido principal. */
@Composable
private fun pinesDeRuta(ruta: Ruta): List<MarcadorMapa> = listOf(
    MarcadorMapa(
        id = "inicio-${ruta.id}",
        punto = ruta.principal.first(),
        tipo = TipoMarcador.Pin("I", UrbanColores.ExitoTexto),
        descripcion = stringResource(Res.string.mapa_pin_inicio),
    ),
    MarcadorMapa(
        id = "final-${ruta.id}",
        punto = ruta.principal.last(),
        tipo = TipoMarcador.Pin("F", UrbanColores.Texto),
        descripcion = stringResource(Res.string.mapa_pin_final),
    ),
)

/** Leyenda: principal = línea SÓLIDA, alterno = línea PUNTEADA (se distingue por forma). */
@Composable
private fun Leyenda(ruta: Ruta) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemLeyenda(stringResource(Res.string.leyenda_principal), colorDe(ruta.colorHex), 6.dp, punteada = false)
        if (ruta.alternos.isNotEmpty()) {
            ItemLeyenda(stringResource(Res.string.leyenda_alterno), UrbanColores.CaminoAlterno, 5.dp, punteada = true)
        }
    }
}

@Composable
private fun ItemLeyenda(texto: String, color: Color, grosor: Dp, punteada: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(width = 32.dp, height = 12.dp)) {
            val g = grosor.toPx()
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = g,
                cap = if (punteada) StrokeCap.Butt else StrokeCap.Round,
                pathEffect = if (punteada) PathEffect.dashPathEffect(floatArrayOf(g * 1.4f, g)) else null,
            )
        }
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ListaBuses(buses: List<BusUi>, modifier: Modifier) {
    Column(modifier) {
        Text(
            stringResource(Res.string.detalle_buses_titulo),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp).semantics { heading() },
        )
        if (buses.isEmpty()) {
            // CA-19.4
            MensajeCentrado(stringResource(Res.string.detalle_sin_buses), Modifier.fillMaxSize())
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(buses, key = { it.id }) { bus ->
                Surface(color = MaterialTheme.colorScheme.surface) {
                    InfoBus(bus, mostrarRuta = false, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MensajeCentrado(texto: String, modifier: Modifier) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
