package co.urbanmovil.app.ui.viaje

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.urbanmovil.app.LocalAppContainer
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.domain.MAX_CAMINATA_TEXTO_M
import co.urbanmovil.app.ui.map.LineaMapa
import co.urbanmovil.app.ui.map.MapaUrbano
import co.urbanmovil.app.ui.map.MarcadorMapa
import co.urbanmovil.app.ui.map.TipoMarcador
import co.urbanmovil.app.ui.map.rememberCamaraMapa
import co.urbanmovil.app.ui.rutas.ControlRelojDebug
import co.urbanmovil.app.ui.rutas.EncabezadoPantalla
import co.urbanmovil.app.ui.rutas.InsigniaRuta
import co.urbanmovil.app.ui.rutas.colorDe
import co.urbanmovil.app.ui.rutas.marcadorMiUbicacion
import co.urbanmovil.app.ui.rutas.marcadoresDeBuses
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.cuando_a11y_lejos
import urbanmovil.shared.generated.resources.cuando_a11y_llega
import urbanmovil.shared.generated.resources.cuando_a11y_sin_bus
import urbanmovil.shared.generated.resources.cuando_lejos
import urbanmovil.shared.generated.resources.cuando_lejos_detalle
import urbanmovil.shared.generated.resources.cuando_lista_titulo
import urbanmovil.shared.generated.resources.cuando_mapa_descripcion
import urbanmovil.shared.generated.resources.cuando_mi_ubicacion
import urbanmovil.shared.generated.resources.cuando_min
import urbanmovil.shared.generated.resources.cuando_nota
import urbanmovil.shared.generated.resources.cuando_pasa_por_lugar
import urbanmovil.shared.generated.resources.cuando_pasa_por_punto
import urbanmovil.shared.generated.resources.cuando_pin_p
import urbanmovil.shared.generated.resources.cuando_punto_mapa
import urbanmovil.shared.generated.resources.cuando_punto_recorrido
import urbanmovil.shared.generated.resources.cuando_sin_bus
import urbanmovil.shared.generated.resources.cuando_sin_bus_sin_reciente
import urbanmovil.shared.generated.resources.cuando_sin_bus_todos_pasaron
import urbanmovil.shared.generated.resources.cuando_sin_bus_uno_paso
import urbanmovil.shared.generated.resources.cuando_subtitulo
import urbanmovil.shared.generated.resources.cuando_subtitulo_mi_ubicacion
import urbanmovil.shared.generated.resources.cuando_titulo
import urbanmovil.shared.generated.resources.ic_mi_ubicacion

private const val ZOOM_INICIAL = 14.0
private const val ZOOM_PUNTO = 15.0

/** Pestaña «¿Cuándo pasa?» (HU-20, mockup «04»). */
@Composable
fun CuandoPasaScreen() {
    val repo = LocalAppContainer.current.transitRepository
    val viewModel = viewModel { CuandoPasaViewModel(repo) }
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    CuandoPasaContenido(
        ui = ui,
        reloj = viewModel.reloj,
        alUsarMiUbicacion = viewModel::usarMiUbicacion,
        alActivarPuntoEnMapa = viewModel::activarPuntoEnMapa,
        alTocarMapa = viewModel::tocarMapa,
    )
}

@Composable
internal fun CuandoPasaContenido(
    ui: CuandoPasaUi,
    reloj: ControlRelojDebug,
    alUsarMiUbicacion: () -> Unit,
    alActivarPuntoEnMapa: () -> Unit,
    alTocarMapa: (Punto) -> Unit,
) {
    val camara = rememberCamaraMapa()
    val idsRutas = ui.rutas.map { it.id }
    LaunchedEffect(idsRutas) { camara.encuadrar(ui.rutas.flatMap { it.principal } + ui.punto, animar = false) }

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(
            titulo = stringResource(Res.string.cuando_titulo),
            subtitulo = stringResource(if (ui.modoMapa) Res.string.cuando_subtitulo else Res.string.cuando_subtitulo_mi_ubicacion),
            reloj = reloj,
        )
        Box(Modifier.fillMaxWidth().weight(1f)) {
            MapaUrbano(
                centro = ui.punto,
                zoom = ZOOM_INICIAL,
                lineas = lineasCuandoPasa(ui),
                marcadores = marcadoresCuandoPasa(ui),
                descripcion = stringResource(Res.string.cuando_mapa_descripcion),
                camara = camara,
                onTocarMapa = alTocarMapa,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !ui.modoMapa,
                    onClick = {
                        alUsarMiUbicacion()
                        camara.centrarEn(ui.miUbicacion.punto, ZOOM_PUNTO)
                    },
                    label = { Text(stringResource(Res.string.cuando_mi_ubicacion)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_mi_ubicacion), contentDescription = null) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                FilterChip(
                    selected = ui.modoMapa,
                    onClick = alActivarPuntoEnMapa,
                    label = { Text(stringResource(Res.string.cuando_punto_mapa)) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
        ListaTarjetas(ui.tarjetas, Modifier.fillMaxWidth().weight(1f))
    }
}

/** Todas las rutas + una línea punteada fina de P al punto del recorrido de cada ruta a ≤ 500 m. */
private fun lineasCuandoPasa(ui: CuandoPasaUi): List<LineaMapa> {
    val rutas = ui.rutas.map { LineaMapa("principal-${it.id}", it.principal, colorDe(it.colorHex), 4.dp, opacidad = 0.8f) }
    val caminatas = ui.tarjetas.mapNotNull { t ->
        val proyeccion = t.proyeccion ?: return@mapNotNull null
        // El id incluye P: si P se mueve, cambia la geometría y la capa se rehace.
        LineaMapa(
            id = "caminata-${t.ruta.id}-${ui.punto.lat}-${ui.punto.lng}",
            puntos = listOf(ui.punto, proyeccion.punto),
            color = UrbanColores.Texto,
            ancho = 2.dp,
            punteada = true,
        )
    }
    return rutas + caminatas
}

@Composable
private fun marcadoresCuandoPasa(ui: CuandoPasaUi): List<MarcadorMapa> {
    val puntosRecorrido = ui.tarjetas.mapNotNull { t ->
        t.proyeccion?.let {
            MarcadorMapa(
                id = "recorrido-${t.ruta.id}",
                punto = it.punto,
                tipo = TipoMarcador.Punto(colorDe(t.ruta.colorHex)),
                descripcion = stringResource(Res.string.cuando_punto_recorrido, t.ruta.id),
            )
        }
    }
    val pinP = MarcadorMapa(
        id = "pin-p",
        punto = ui.punto,
        tipo = TipoMarcador.Pin("P", UrbanColores.PinRojo),
        descripcion = stringResource(Res.string.cuando_pin_p),
    )
    val miUbicacion = if (ui.enMiUbicacion) emptyList() else listOf(marcadorMiUbicacion(ui.miUbicacion))
    return miUbicacion + marcadoresDeBuses(ui.buses) + puntosRecorrido + pinP
}

@Composable
private fun ListaTarjetas(tarjetas: List<TarjetaEta>, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), shadowElevation = 8.dp) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    Text(
                        stringResource(Res.string.cuando_lista_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(Res.string.cuando_nota),
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColores.AvisoTexto,
                    )
                }
            }
            items(tarjetas, key = { it.ruta.id }) { TarjetaEtaVista(it) }
        }
    }
}

@Composable
private fun TarjetaEtaVista(tarjeta: TarjetaEta) {
    val ruta = tarjeta.ruta
    val (detalle, a11y) = when (val e = tarjeta.estado) {
        is EstadoEtaUi.Llega -> {
            val texto = e.lugar?.let { stringResource(Res.string.cuando_pasa_por_lugar, it, e.distanciaM) }
                ?: stringResource(Res.string.cuando_pasa_por_punto, e.distanciaM)
            texto to stringResource(Res.string.cuando_a11y_llega, ruta.id, e.minutos) + ". " + texto
        }
        is EstadoEtaUi.SinBus -> {
            val motivo = stringResource(
                when {
                    e.busesRecientes == 0 -> Res.string.cuando_sin_bus_sin_reciente
                    e.busesRecientes == 1 -> Res.string.cuando_sin_bus_uno_paso
                    else -> Res.string.cuando_sin_bus_todos_pasaron
                },
            )
            motivo to stringResource(Res.string.cuando_a11y_sin_bus, ruta.id, motivo)
        }
        EstadoEtaUi.Lejos -> {
            val texto = stringResource(Res.string.cuando_lejos_detalle, MAX_CAMINATA_TEXTO_M)
            texto to stringResource(Res.string.cuando_a11y_lejos, ruta.id) + ". " + texto
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        // Una sola unidad para TalkBack: «Ruta R1, llega en 6 minutos aproximadamente…».
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = a11y },
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InsigniaRuta(ruta.id, colorDe(ruta.colorHex))
            Column(Modifier.weight(1f)) {
                Text(ruta.nombre, style = MaterialTheme.typography.titleSmall)
                val titulo = when (tarjeta.estado) {
                    is EstadoEtaUi.SinBus -> stringResource(Res.string.cuando_sin_bus)
                    EstadoEtaUi.Lejos -> stringResource(Res.string.cuando_lejos)
                    is EstadoEtaUi.Llega -> null
                }
                titulo?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            (tarjeta.estado as? EstadoEtaUi.Llega)?.let {
                Text(
                    stringResource(Res.string.cuando_min, it.minutos),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
