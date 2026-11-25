package co.urbanmovil.app.ui.viaje

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.urbanmovil.app.LocalAppContainer
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.domain.MAX_CAMINATA_TEXTO_M
import co.urbanmovil.app.ui.map.LineaMapa
import co.urbanmovil.app.ui.map.MapaUrbano
import co.urbanmovil.app.ui.map.rememberCamaraMapa
import co.urbanmovil.app.ui.rutas.ControlRelojDebug
import co.urbanmovil.app.ui.rutas.EncabezadoPantalla
import co.urbanmovil.app.ui.rutas.InsigniaRuta
import co.urbanmovil.app.ui.rutas.colorDe
import co.urbanmovil.app.ui.rutas.marcadorMiUbicacion
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.buscar_a11y_llega
import urbanmovil.shared.generated.resources.buscar_a11y_opcion
import urbanmovil.shared.generated.resources.buscar_a11y_sin_bus
import urbanmovil.shared.generated.resources.buscar_abrir_guia
import urbanmovil.shared.generated.resources.buscar_caminatas
import urbanmovil.shared.generated.resources.buscar_cancelar
import urbanmovil.shared.generated.resources.buscar_destino
import urbanmovil.shared.generated.resources.buscar_elegir_mapa
import urbanmovil.shared.generated.resources.buscar_elegir_titulo_destino
import urbanmovil.shared.generated.resources.buscar_elegir_titulo_origen
import urbanmovil.shared.generated.resources.buscar_intercambiar
import urbanmovil.shared.generated.resources.buscar_llega_en
import urbanmovil.shared.generated.resources.buscar_mapa_descripcion
import urbanmovil.shared.generated.resources.buscar_mapa_instruccion
import urbanmovil.shared.generated.resources.buscar_menos_caminata
import urbanmovil.shared.generated.resources.buscar_mi_ubicacion
import urbanmovil.shared.generated.resources.buscar_nota_pie
import urbanmovil.shared.generated.resources.buscar_orden_nota
import urbanmovil.shared.generated.resources.buscar_origen
import urbanmovil.shared.generated.resources.buscar_puntos_iguales
import urbanmovil.shared.generated.resources.buscar_resultado_titulo
import urbanmovil.shared.generated.resources.buscar_sin_rutas
import urbanmovil.shared.generated.resources.buscar_subtitulo
import urbanmovil.shared.generated.resources.buscar_titulo
import urbanmovil.shared.generated.resources.buscar_trayecto
import urbanmovil.shared.generated.resources.cuando_sin_bus
import urbanmovil.shared.generated.resources.ic_buscar
import urbanmovil.shared.generated.resources.ic_chevron
import urbanmovil.shared.generated.resources.ic_intercambiar
import urbanmovil.shared.generated.resources.ic_mi_ubicacion

/** Qué campo se está eligiendo. */
private enum class Campo { Origen, Destino }

/**
 * Pestaña «Buscar viaje» (HU-21, HU-22, mockup «05»).
 * @param viewModel compartido con la guía; lo crea el grafo de navegación en la entrada de esta pestaña.
 */
@Composable
fun BuscarViajeScreen(alAbrirGuia: (String) -> Unit, viewModel: BuscarViajeViewModel = buscarViajeViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var hojaAbierta by rememberSaveable { mutableStateOf<Campo?>(null) }
    var eligiendoEnMapa by rememberSaveable { mutableStateOf<Campo?>(null) }

    val elegir: (Campo, PuntoElegido) -> Unit = { campo, punto ->
        if (campo == Campo.Origen) viewModel.elegirOrigen(punto) else viewModel.elegirDestino(punto)
    }

    eligiendoEnMapa?.let { campo ->
        SelectorEnMapa(
            titulo = stringResource(if (campo == Campo.Origen) Res.string.buscar_elegir_titulo_origen else Res.string.buscar_elegir_titulo_destino),
            inicial = if (campo == Campo.Origen) ui.origen.punto else ui.destino.punto,
            rutas = viewModel.rutas,
            miUbicacion = viewModel.miUbicacion,
            reloj = viewModel.reloj,
            alElegir = { p ->
                elegir(campo, PuntoElegido.EnMapa(p))
                eligiendoEnMapa = null
            },
            alCancelar = { eligiendoEnMapa = null },
        )
        return
    }

    BuscarViajeContenido(
        ui = ui,
        reloj = viewModel.reloj,
        alTocarCampo = { hojaAbierta = it },
        alIntercambiar = viewModel::intercambiar,
        alAbrirGuia = alAbrirGuia,
    )

    hojaAbierta?.let { campo ->
        HojaElegirPunto(
            titulo = stringResource(if (campo == Campo.Origen) Res.string.buscar_elegir_titulo_origen else Res.string.buscar_elegir_titulo_destino),
            miUbicacion = viewModel.miUbicacion,
            lugares = viewModel.lugares,
            alElegir = { punto ->
                elegir(campo, punto)
                hojaAbierta = null
            },
            alElegirEnMapa = {
                hojaAbierta = null
                eligiendoEnMapa = campo
            },
            alCerrar = { hojaAbierta = null },
        )
    }
}

/**
 * ViewModel de la búsqueda en el `ViewModelStoreOwner` actual (la entrada de la pestaña en el back
 * stack). La guía pide el mismo con `viewModelStoreOwner = <entrada de la pestaña>`, así ambas
 * pantallas comparten instancia.
 */
@Composable
internal fun buscarViajeViewModel(): BuscarViajeViewModel {
    val repo = LocalAppContainer.current.transitRepository
    return viewModel { BuscarViajeViewModel(repo) }
}

@Composable
private fun BuscarViajeContenido(
    ui: BuscarViajeUi,
    reloj: ControlRelojDebug,
    alTocarCampo: (Campo) -> Unit,
    alIntercambiar: () -> Unit,
    alAbrirGuia: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(
            titulo = stringResource(Res.string.buscar_titulo),
            subtitulo = stringResource(Res.string.buscar_subtitulo, MAX_CAMINATA_TEXTO_M),
            reloj = reloj,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CamposOrigenDestino(ui, alTocarCampo, alIntercambiar) }
            when (val r = ui.resultado) {
                ResultadoBusquedaUi.PuntosIguales -> item { Mensaje(stringResource(Res.string.buscar_puntos_iguales)) }
                is ResultadoBusquedaUi.Opciones -> if (r.opciones.isEmpty()) {
                    item { Mensaje(stringResource(Res.string.buscar_sin_rutas)) }
                } else {
                    item {
                        Column {
                            Text(
                                pluralStringResource(Res.plurals.buscar_resultado_titulo, r.opciones.size, r.opciones.size),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                stringResource(Res.string.buscar_orden_nota),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(r.opciones, key = { it.id }) { TarjetaOpcion(it, alTocar = { alAbrirGuia(it.id) }) }
                    item {
                        Text(
                            stringResource(Res.string.buscar_nota_pie),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CamposOrigenDestino(ui: BuscarViajeUi, alTocarCampo: (Campo) -> Unit, alIntercambiar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                CampoPunto(stringResource(Res.string.buscar_origen), nombrePunto(ui.origen)) { alTocarCampo(Campo.Origen) }
                HorizontalDivider(Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                CampoPunto(stringResource(Res.string.buscar_destino), nombrePunto(ui.destino)) { alTocarCampo(Campo.Destino) }
            }
            FilledTonalIconButton(onClick = alIntercambiar) {
                Icon(painterResource(Res.drawable.ic_intercambiar), contentDescription = stringResource(Res.string.buscar_intercambiar))
            }
        }
    }
}

@Composable
private fun CampoPunto(etiqueta: String, valor: String, alTocar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
    }
}

@Composable
private fun TarjetaOpcion(opcion: OpcionViajeUi, alTocar: () -> Unit) {
    val ruta = opcion.ruta
    val caminatas = stringResource(Res.string.buscar_caminatas, opcion.caminataAbordajeM, opcion.caminataDescensoM)
    val llegada = opcion.etaMin?.let { stringResource(Res.string.buscar_a11y_llega, it) } ?: stringResource(Res.string.buscar_a11y_sin_bus)
    val a11y = stringResource(Res.string.buscar_a11y_opcion, ruta.id, llegada, caminatas, opcion.trayectoMin)
    val accion = stringResource(Res.string.buscar_abrir_guia)

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (opcion.menosCaminata) 2.dp else 1.dp, if (opcion.menosCaminata) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alTocar)
            .clearAndSetSemantics {
                contentDescription = a11y
                role = Role.Button
                onClick(label = accion) { alTocar(); true }
            },
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsigniaRuta(ruta.id, colorDe(ruta.colorHex))
                Text(ruta.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(painterResource(Res.drawable.ic_chevron), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(caminatas, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (opcion.menosCaminata) Chip(stringResource(Res.string.buscar_menos_caminata), EstiloChip.Destacado)
                opcion.etaMin?.let { Chip(stringResource(Res.string.buscar_llega_en, it), EstiloChip.Tiempo) }
                    ?: Chip(stringResource(Res.string.cuando_sin_bus), EstiloChip.Neutro)
                Chip(stringResource(Res.string.buscar_trayecto, opcion.trayectoMin), EstiloChip.Neutro)
            }
        }
    }
}

@Composable
private fun Mensaje(texto: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaElegirPunto(
    titulo: String,
    miUbicacion: Lugar,
    lugares: List<Lugar>,
    alElegir: (PuntoElegido) -> Unit,
    alElegirEnMapa: () -> Unit,
    alCerrar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = alCerrar) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding()) {
            item {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { heading() },
                )
            }
            item {
                FilaHoja(stringResource(Res.string.buscar_mi_ubicacion), Res.drawable.ic_mi_ubicacion) {
                    alElegir(PuntoElegido.MiUbicacion(miUbicacion.punto))
                }
            }
            items(lugares, key = { it.nombre }) { lugar ->
                FilaHoja(lugar.nombre, null) { alElegir(PuntoElegido.DeLugar(lugar)) }
            }
            item { FilaHoja(stringResource(Res.string.buscar_elegir_mapa), Res.drawable.ic_buscar, alTocar = alElegirEnMapa) }
        }
    }
}

@Composable
private fun FilaHoja(texto: String, icono: DrawableResource?, alTocar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icono != null) {
            Icon(painterResource(icono), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Mapa a pantalla completa: tocar fija el punto y vuelve a la búsqueda. */
@Composable
private fun SelectorEnMapa(
    titulo: String,
    inicial: Punto,
    rutas: List<Ruta>,
    miUbicacion: Lugar,
    reloj: ControlRelojDebug,
    alElegir: (Punto) -> Unit,
    alCancelar: () -> Unit,
) {
    val camara = rememberCamaraMapa()
    LaunchedEffect(Unit) { camara.encuadrar(rutas.flatMap { it.principal } + inicial, animar = false) }

    Column(Modifier.fillMaxSize()) {
        EncabezadoPantalla(titulo = titulo, subtitulo = stringResource(Res.string.buscar_mapa_instruccion), reloj = reloj, alVolver = alCancelar)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            MapaUrbano(
                centro = inicial,
                zoom = 14.0,
                // Las rutas de referencia ayudan a elegir un punto a ≤ 500 m de alguna.
                lineas = rutas.map { LineaMapa("ref-${it.id}", it.principal, colorDe(it.colorHex), 3.dp, opacidad = 0.6f) },
                marcadores = listOf(marcadorMiUbicacion(miUbicacion)),
                descripcion = stringResource(Res.string.buscar_mapa_descripcion),
                camara = camara,
                onTocarMapa = alElegir,
                modifier = Modifier.fillMaxSize(),
            )
            OutlinedButton(
                onClick = alCancelar,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
            ) { Text(stringResource(Res.string.buscar_cancelar)) }
        }
    }
}
