package co.urbanmovil.app.ui.rutas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.urbanmovil.app.LocalEsDebug
import co.urbanmovil.app.data.model.NivelOcupacion
import co.urbanmovil.app.domain.InfoOcupacion
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.bus_codigo
import urbanmovil.shared.generated.resources.bus_ruta
import urbanmovil.shared.generated.resources.bus_ultima_ubicacion
import urbanmovil.shared.generated.resources.debug_adelantar_reloj
import urbanmovil.shared.generated.resources.debug_reloj
import urbanmovil.shared.generated.resources.detalle_volver
import urbanmovil.shared.generated.resources.ic_atras
import urbanmovil.shared.generated.resources.ic_cerrar
import urbanmovil.shared.generated.resources.ocupacion_aviso
import urbanmovil.shared.generated.resources.ocupacion_con_puestos
import urbanmovil.shared.generated.resources.ocupacion_lleno
import urbanmovil.shared.generated.resources.ocupacion_reportada
import urbanmovil.shared.generated.resources.ocupacion_sin_info
import urbanmovil.shared.generated.resources.ocupacion_solo_de_pie
import urbanmovil.shared.generated.resources.tarjeta_cerrar
import urbanmovil.shared.generated.resources.vigencia_desactualizada
import urbanmovil.shared.generated.resources.vigencia_reciente

/** Color ARGB guardado como Long en el modelo ([co.urbanmovil.app.data.model.Ruta.colorHex]). */
internal fun colorDe(argb: Long): Color = Color(argb)

/**
 * Barra superior. En build debug, mantener presionado el título adelanta el reloj simulado
 * ([ControlRelojDebug.MINUTOS_POR_TOQUE] min) para enseñar buses desactualizados u ocultos.
 */
@Composable
internal fun EncabezadoPantalla(
    titulo: String,
    subtitulo: String,
    reloj: ControlRelojDebug,
    alVolver: (() -> Unit)? = null,
) {
    val esDebug = LocalEsDebug.current
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (alVolver != null) {
                IconButton(onClick = alVolver) {
                    Icon(painterResource(Res.drawable.ic_atras), contentDescription = stringResource(Res.string.detalle_volver))
                }
            }
            val etiquetaDebug = stringResource(Res.string.debug_adelantar_reloj, ControlRelojDebug.MINUTOS_POR_TOQUE)
            val toqueDebug = if (esDebug) {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClickLabel = etiquetaDebug,
                    onLongClick = { reloj.adelantar() },
                )
            } else {
                Modifier
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp).then(toqueDebug)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (esDebug) AvisoRelojDebug(reloj)
            }
        }
    }
}

@Composable
private fun AvisoRelojDebug(reloj: ControlRelojDebug) {
    val ajusteMs by reloj.ajusteMs.collectAsStateWithLifecycle()
    if (ajusteMs == 0L) return
    Text(
        stringResource(Res.string.debug_reloj, (ajusteMs / 60_000L).toInt()),
        style = MaterialTheme.typography.labelMedium,
        color = UrbanColores.AvisoTexto,
        modifier = Modifier
            .padding(top = 4.dp)
            .background(UrbanColores.AvisoFondo, RoundedCornerShape(6.dp))
            .clickable { reloj.reiniciar() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Insignia «R1» con el color de la ruta. */
@Composable
internal fun InsigniaRuta(codigo: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.sizeIn(minWidth = 44.dp, minHeight = 32.dp).background(color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            codigo,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** Chip de vigencia: además del color, cambia el texto y la forma del punto (relleno vs. hueco). */
@Composable
internal fun ChipVigencia(desactualizado: Boolean) {
    val (fondo, texto) = if (desactualizado) {
        Color(0xFFF1F2F4) to UrbanColores.Desactualizado
    } else {
        UrbanColores.ExitoFondo to UrbanColores.ExitoTexto
    }
    Row(
        Modifier.background(fondo, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val punto = Modifier.size(8.dp)
        Box(if (desactualizado) punto.border(1.5.dp, texto, CircleShape) else punto.background(texto, CircleShape))
        Text(
            stringResource(if (desactualizado) Res.string.vigencia_desactualizada else Res.string.vigencia_reciente),
            style = MaterialTheme.typography.labelMedium,
            color = texto,
        )
    }
}

/** HU-24. Con la bandera apagada ([InfoOcupacion.Oculta]) no dibuja NADA. */
@Composable
internal fun BloqueOcupacion(info: InfoOcupacion) {
    when (info) {
        InfoOcupacion.Oculta -> Unit
        InfoOcupacion.SinInformacion -> Text(
            stringResource(Res.string.ocupacion_sin_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is InfoOcupacion.Vigente -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    textoNivel(info.nivel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Text(
                    stringResource(Res.string.ocupacion_reportada, info.haceMin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(Res.string.ocupacion_aviso), style = MaterialTheme.typography.bodySmall, color = UrbanColores.AvisoTexto)
        }
    }
}

@Composable
private fun textoNivel(nivel: NivelOcupacion): String = stringResource(
    when (nivel) {
        NivelOcupacion.CON_PUESTOS -> Res.string.ocupacion_con_puestos
        NivelOcupacion.SOLO_DE_PIE -> Res.string.ocupacion_solo_de_pie
        NivelOcupacion.LLENO -> Res.string.ocupacion_lleno
    },
)

/** Código, vigencia, hora de la última ubicación y ocupación de un bus. Se usa en lista y tarjeta. */
@Composable
internal fun InfoBus(bus: BusUi, mostrarRuta: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.bus_codigo, bus.id), style = MaterialTheme.typography.titleMedium)
            ChipVigencia(bus.desactualizado)
        }
        if (mostrarRuta) {
            Text(
                stringResource(Res.string.bus_ruta, bus.rutaId, bus.rutaNombre),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            stringResource(Res.string.bus_ultima_ubicacion, bus.horaUltimaUbicacion, bus.edadUbicacion),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BloqueOcupacion(bus.ocupacion)
    }
}

/** Tarjeta que aparece al tocar un marcador de bus. */
@Composable
internal fun TarjetaBus(bus: BusUi, alCerrar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            InfoBus(bus, mostrarRuta = true, modifier = Modifier.weight(1f))
            IconButton(onClick = alCerrar) {
                Icon(painterResource(Res.drawable.ic_cerrar), contentDescription = stringResource(Res.string.tarjeta_cerrar))
            }
        }
    }
}
