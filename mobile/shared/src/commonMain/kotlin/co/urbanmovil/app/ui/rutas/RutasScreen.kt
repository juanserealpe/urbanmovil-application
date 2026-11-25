package co.urbanmovil.app.ui.rutas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.urbanmovil.app.LocalAppContainer
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.ui.map.CamaraMapa
import co.urbanmovil.app.ui.map.LineaMapa
import co.urbanmovil.app.ui.map.MapaUrbano
import co.urbanmovil.app.ui.map.rememberCamaraMapa
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.ic_chevron
import urbanmovil.shared.generated.resources.ic_mi_ubicacion
import urbanmovil.shared.generated.resources.mapa_rutas_descripcion
import urbanmovil.shared.generated.resources.rutas_abrir_detalle
import urbanmovil.shared.generated.resources.rutas_buses_activos
import urbanmovil.shared.generated.resources.rutas_centrar
import urbanmovil.shared.generated.resources.rutas_sin_buses
import urbanmovil.shared.generated.resources.rutas_subtitulo
import urbanmovil.shared.generated.resources.rutas_titulo
import urbanmovil.shared.generated.resources.rutas_vacio

private const val ZOOM_CIUDAD = 13.5
private const val ZOOM_MI_UBICACION = 15.5

/** Pestaña Rutas (HU-18, mockup «02 · Rutas»). */
@Composable
fun RutasScreen(alAbrirRuta: (String) -> Unit) {
    val repo = LocalAppContainer.current.transitRepository
    val viewModel = viewModel { RutasViewModel(repo) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    RutasContenido(
        ui = ui,
        reloj = viewModel.reloj,
        alTocarRuta = { rutaId ->
            viewModel.seleccionarRuta(rutaId)
            alAbrirRuta(rutaId)
        },
        alTocarBus = { viewModel.tocarBus(it) },
    )
}

@Composable
internal fun RutasContenido(
    ui: RutasUi,
    reloj: ControlRelojDebug,
    alTocarRuta: (String) -> Unit,
    alTocarBus: (String?) -> Unit,
) {
    val camara = rememberCamaraMapa()
    // Se encuadra al entrar (y si cambia el conjunto de rutas), NO en cada tick de los buses.
    val idsRutas = ui.rutas.map { it.ruta.id }
    LaunchedEffect(idsRutas) { camara.encuadrar(ui.rutas.flatMap { it.ruta.principal }, animar = false) }

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(
            titulo = stringResource(Res.string.rutas_titulo),
            subtitulo = pluralStringResource(Res.plurals.rutas_subtitulo, ui.rutas.size, ui.rutas.size),
            reloj = reloj,
        )
        MapaDeRutas(ui, camara, alTocarBus, Modifier.fillMaxWidth().weight(1f))
        ListaRutas(ui, alTocarRuta, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun MapaDeRutas(ui: RutasUi, camara: CamaraMapa, alTocarBus: (String?) -> Unit, modifier: Modifier) {
    val lineas = remember(ui.rutas, ui.seleccionadaId) { lineasDeRutas(ui.rutas.map { it.ruta }, ui.seleccionadaId) }
    val marcadores = marcadoresDeBuses(ui.buses) { alTocarBus(it.id) } + marcadorMiUbicacion(ui.miUbicacion)

    Box(modifier) {
        MapaUrbano(
            centro = ui.miUbicacion.punto,
            zoom = ZOOM_CIUDAD,
            lineas = lineas,
            marcadores = marcadores,
            descripcion = stringResource(Res.string.mapa_rutas_descripcion),
            camara = camara,
            modifier = Modifier.fillMaxSize(),
        )
        // Por encima de los botones de atribución de la esquina inferior.
        FloatingActionButton(
            onClick = { camara.centrarEn(ui.miUbicacion.punto, ZOOM_MI_UBICACION) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 56.dp),
        ) {
            Icon(painterResource(Res.drawable.ic_mi_ubicacion), contentDescription = stringResource(Res.string.rutas_centrar))
        }
        ui.busSeleccionado?.let { bus ->
            TarjetaBus(bus, alCerrar = { alTocarBus(null) }, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp))
        }
    }
}

/**
 * Seleccionada: su color, encima de las demás, con sus alternos punteados.
 * Las demás: gris semitransparente.
 */
internal fun lineasDeRutas(rutas: List<Ruta>, seleccionadaId: String?): List<LineaMapa> {
    val (seleccionadas, otras) = rutas.partition { it.id == seleccionadaId }
    val grises = otras.map { ruta ->
        // Id distinto al de la resaltada: al cambiar la selección la capa se vuelve a montar encima.
        LineaMapa("gris-${ruta.id}", ruta.principal, UrbanColores.DesactualizadoSuave, 4.dp, opacidad = 0.45f)
    }
    val resaltadas = seleccionadas.flatMap { ruta -> lineasDeRuta(ruta) }
    return grises + resaltadas
}

/** Recorrido principal sólido (6.5 dp, color de la ruta) y alternos punteados naranja (5 dp). */
internal fun lineasDeRuta(ruta: Ruta): List<LineaMapa> =
    listOf(LineaMapa("principal-${ruta.id}", ruta.principal, colorDe(ruta.colorHex), 6.5.dp)) +
        ruta.alternos.mapIndexed { i, alterno ->
            LineaMapa("alterno-${ruta.id}-$i", alterno, UrbanColores.CaminoAlterno, 5.dp, punteada = true)
        }

@Composable
private fun ListaRutas(ui: RutasUi, alTocarRuta: (String) -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        if (ui.rutas.isEmpty()) {
            // CA-18.3
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.rutas_vacio),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Surface
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(ui.rutas, key = { it.ruta.id }) { resumen ->
                FilaRuta(resumen, seleccionada = resumen.ruta.id == ui.seleccionadaId, alTocar = { alTocarRuta(resumen.ruta.id) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun FilaRuta(resumen: ResumenRuta, seleccionada: Boolean, alTocar: () -> Unit) {
    val fondo = if (seleccionada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    Surface(color = fondo) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(Res.string.rutas_abrir_detalle), role = Role.Button, onClick = alTocar)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InsigniaRuta(resumen.ruta.id, colorDe(resumen.ruta.colorHex))
            Column(Modifier.weight(1f)) {
                Text(resumen.ruta.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    textoBusesActivos(resumen.busesActivos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(painterResource(Res.drawable.ic_chevron), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun textoBusesActivos(n: Int): String =
    if (n == 0) stringResource(Res.string.rutas_sin_buses) else pluralStringResource(Res.plurals.rutas_buses_activos, n, n)
