package co.urbanmovil.app.ui.viaje

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.urbanmovil.app.domain.tramoEntre
import co.urbanmovil.app.ui.map.LineaMapa
import co.urbanmovil.app.ui.map.MapaUrbano
import co.urbanmovil.app.ui.map.MarcadorMapa
import co.urbanmovil.app.ui.map.TipoMarcador
import co.urbanmovil.app.ui.map.rememberCamaraMapa
import co.urbanmovil.app.ui.rutas.ControlRelojDebug
import co.urbanmovil.app.ui.rutas.EncabezadoPantalla
import co.urbanmovil.app.ui.rutas.colorDe
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.buscar_trayecto
import urbanmovil.shared.generated.resources.cuando_sin_bus
import urbanmovil.shared.generated.resources.guia_bus_llega
import urbanmovil.shared.generated.resources.guia_cerca_de
import urbanmovil.shared.generated.resources.guia_destino_mapa
import urbanmovil.shared.generated.resources.guia_destino_mi_ubicacion
import urbanmovil.shared.generated.resources.guia_hacia
import urbanmovil.shared.generated.resources.guia_mapa_descripcion
import urbanmovil.shared.generated.resources.guia_no_disponible
import urbanmovil.shared.generated.resources.guia_otras_opciones
import urbanmovil.shared.generated.resources.guia_paso1
import urbanmovil.shared.generated.resources.guia_paso2
import urbanmovil.shared.generated.resources.guia_paso2_detalle
import urbanmovil.shared.generated.resources.guia_paso2_detalle_lugar
import urbanmovil.shared.generated.resources.guia_paso3
import urbanmovil.shared.generated.resources.guia_pin_a
import urbanmovil.shared.generated.resources.guia_pin_b
import urbanmovil.shared.generated.resources.guia_pin_d
import urbanmovil.shared.generated.resources.guia_pin_origen
import urbanmovil.shared.generated.resources.guia_punto_recorrido
import urbanmovil.shared.generated.resources.guia_titulo

/**
 * Guía de una opción (HU-23, mockup «06»). Usa el MISMO [BuscarViajeViewModel] que la lista, así los
 * tiempos siguen en vivo y «Ver otras opciones» vuelve con la búsqueda intacta (CA-23.2).
 */
@Composable
fun GuiaScreen(opcionId: String, viewModel: BuscarViajeViewModel, alVolver: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val opcion = (ui.resultado as? ResultadoBusquedaUi.Opciones)?.opciones?.firstOrNull { it.id == opcionId }
    GuiaContenido(ui, opcion, viewModel.reloj, alVolver)
}

@Composable
private fun GuiaContenido(ui: BuscarViajeUi, opcion: OpcionViajeUi?, reloj: ControlRelojDebug, alVolver: () -> Unit) {
    if (opcion == null) {
        Column(Modifier.fillMaxSize()) {
            EncabezadoPantalla(titulo = "", subtitulo = "", reloj = reloj, alVolver = alVolver)
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.guia_no_disponible), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    val ruta = opcion.ruta
    val destinoTexto = when (val d = ui.destino) {
        is PuntoElegido.DeLugar -> d.lugar.nombre
        is PuntoElegido.EnMapa -> stringResource(Res.string.guia_destino_mapa)
        is PuntoElegido.MiUbicacion -> stringResource(Res.string.guia_destino_mi_ubicacion)
    }

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(
            titulo = stringResource(Res.string.guia_titulo, ruta.id),
            subtitulo = stringResource(Res.string.guia_hacia, destinoTexto),
            reloj = reloj,
            alVolver = alVolver,
        )
        MapaGuia(ui, opcion, Modifier.fillMaxWidth().weight(1f))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    opcion.etaMin?.let { Chip(stringResource(Res.string.guia_bus_llega, it), EstiloChip.Tiempo) }
                        ?: Chip(stringResource(Res.string.cuando_sin_bus), EstiloChip.Neutro)
                    Chip(stringResource(Res.string.buscar_trayecto, opcion.trayectoMin), EstiloChip.Neutro)
                }
            }
            item {
                Paso(
                    numero = 1,
                    titulo = stringResource(Res.string.guia_paso1, opcion.caminataAbordajeM),
                    detalle = opcion.lugarAbordaje?.let { stringResource(Res.string.guia_cerca_de, it) }
                        ?: stringResource(Res.string.guia_punto_recorrido, ruta.id),
                    color = UrbanColores.ExitoTexto,
                )
            }
            item {
                Paso(
                    numero = 2,
                    titulo = stringResource(Res.string.guia_paso2, ruta.id, ruta.nombre),
                    detalle = opcion.lugarDescenso?.let { stringResource(Res.string.guia_paso2_detalle_lugar, it) }
                        ?: stringResource(Res.string.guia_paso2_detalle),
                    color = colorDe(ruta.colorHex),
                )
            }
            item {
                Paso(
                    numero = 3,
                    titulo = stringResource(Res.string.guia_paso3, opcion.caminataDescensoM),
                    detalle = destinoTexto,
                    color = UrbanColores.Texto,
                )
            }
            item {
                OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.guia_otras_opciones))
                }
            }
        }
    }
}

@Composable
private fun MapaGuia(ui: BuscarViajeUi, opcion: OpcionViajeUi, modifier: Modifier) {
    val ruta = opcion.ruta
    val abordaje = opcion.opcion.abordaje
    val descenso = opcion.opcion.descenso
    val origen = ui.origen.punto
    val destino = ui.destino.punto

    val tramoBus = remember(ruta.id, abordaje.posicionM, descenso.posicionM) {
        tramoEntre(ruta.principal, abordaje.posicionM, descenso.posicionM)
    }
    val camara = rememberCamaraMapa()
    LaunchedEffect(opcion.id, origen, destino) {
        camara.encuadrar(tramoBus + origen + destino, animar = false)
    }

    // Tres estilos distintos (CA-23.1): ruta tenue de fondo, tramo en bus sólido y grueso,
    // tramos a pie punteados, finos y oscuros. Los ids incluyen la geometría.
    val clave = "${opcion.id}-${origen.lat},${origen.lng}-${destino.lat},${destino.lng}"
    val lineas = listOf(
        LineaMapa("fondo-${ruta.id}", ruta.principal, colorDe(ruta.colorHex), 4.dp, opacidad = 0.3f),
        LineaMapa("bus-$clave", tramoBus, colorDe(ruta.colorHex), 7.dp),
        LineaMapa("pie-a-$clave", listOf(origen, abordaje.punto), UrbanColores.Texto, 2.5.dp, punteada = true),
        LineaMapa("pie-d-$clave", listOf(descenso.punto, destino), UrbanColores.Texto, 2.5.dp, punteada = true),
    )
    val marcadores = listOf(
        MarcadorMapa("origen", origen, TipoMarcador.Punto(UrbanColores.Primario, grande = true), stringResource(Res.string.guia_pin_origen)),
        MarcadorMapa("pin-a", abordaje.punto, TipoMarcador.Pin("A", UrbanColores.ExitoTexto), stringResource(Res.string.guia_pin_a)),
        MarcadorMapa("pin-b", descenso.punto, TipoMarcador.Pin("B", UrbanColores.PinRojo), stringResource(Res.string.guia_pin_b)),
        MarcadorMapa("pin-d", destino, TipoMarcador.Pin("D", UrbanColores.Texto), stringResource(Res.string.guia_pin_d)),
    )

    MapaUrbano(
        centro = abordaje.punto,
        zoom = 14.0,
        lineas = lineas,
        marcadores = marcadores,
        descripcion = stringResource(Res.string.guia_mapa_descripcion, ruta.id),
        camara = camara,
        modifier = modifier,
    )
}

@Composable
private fun Paso(numero: Int, titulo: String, detalle: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(28.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Text(numero.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
