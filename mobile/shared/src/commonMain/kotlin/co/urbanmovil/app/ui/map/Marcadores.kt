package co.urbanmovil.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.painterResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.ic_bus

/** Dibuja un marcador. [modifier] trae la ubicación en el mapa y debe ir en el nodo raíz. */
@Composable
internal fun MarcadorVista(marcador: MarcadorMapa, etiquetaDesactualizado: String, modifier: Modifier = Modifier) {
    val conToque = marcador.alTocar?.let { modifier.clickable(role = Role.Button, onClick = it) } ?: modifier
    // Una sola etiqueta para el lector de pantalla; el ícono y el texto interno no se leen sueltos.
    val tocable = conToque.clearAndSetSemantics {
        contentDescription = marcador.descripcion
        marcador.alTocar?.let { accion ->
            role = Role.Button
            onClick { accion(); true }
        }
    }

    when (val tipo = marcador.tipo) {
        is TipoMarcador.Bus -> MarcadorBus(tipo, etiquetaDesactualizado, tocable)
        is TipoMarcador.Pin -> MarcadorPin(tipo, tocable)
        TipoMarcador.MiUbicacion -> MarcadorMiUbicacion(tocable)
        is TipoMarcador.Punto -> MarcadorPunto(tipo, tocable)
    }
}

/**
 * RECIENTE: círculo relleno del color de la ruta, borde blanco, bus blanco.
 * DESACTUALIZADA: círculo blanco con borde gris PUNTEADO, bus gris y etiqueta «Desactualizado»
 * (la diferencia no depende solo del color, RNF accesibilidad).
 */
@Composable
private fun MarcadorBus(tipo: TipoMarcador.Bus, etiquetaDesactualizado: String, modifier: Modifier) {
    // 40 dp de área táctil alrededor de un círculo de 32 dp.
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        val circulo = if (tipo.desactualizado) {
            Modifier.size(32.dp).background(Color.White, CircleShape).drawBehind {
                val grosor = 2.dp.toPx()
                drawCircle(
                    color = UrbanColores.DesactualizadoSuave,
                    radius = size.minDimension / 2 - grosor / 2,
                    style = Stroke(
                        width = grosor,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
        } else {
            Modifier.size(32.dp)
                .background(tipo.colorRuta, CircleShape)
                .border(2.dp, Color.White, CircleShape)
        }
        Box(circulo, contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.ic_bus),
                contentDescription = null,
                tint = if (tipo.desactualizado) UrbanColores.DesactualizadoSuave else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        if (tipo.desactualizado) {
            Text(
                text = etiquetaDesactualizado,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                // La etiqueta sobresale a la derecha del círculo sin agrandar el área del marcador.
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 40.dp)
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .background(UrbanColores.Desactualizado, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Pin con letra y punta hacia abajo; se ancla por la punta (BottomCenter). */
@Composable
private fun MarcadorPin(tipo: TipoMarcador.Pin, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(28.dp).background(tipo.color, CircleShape).border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(tipo.letra, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.size(width = 3.dp, height = 8.dp).background(tipo.color))
    }
}

@Composable
private fun MarcadorMiUbicacion(modifier: Modifier) {
    Box(
        modifier.size(26.dp).background(UrbanColores.Primario.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(14.dp).background(UrbanColores.Primario, CircleShape).border(2.dp, Color.White, CircleShape))
    }
}

@Composable
private fun MarcadorPunto(tipo: TipoMarcador.Punto, modifier: Modifier) {
    val tamano = if (tipo.grande) 18.dp else 12.dp
    Box(modifier.size(tamano).background(tipo.color, CircleShape).border(2.dp, Color.White, CircleShape))
}

/** Dónde se ancla cada tipo de marcador respecto a su coordenada. */
internal fun TipoMarcador.anclaje(): Alignment = when (this) {
    is TipoMarcador.Pin -> Alignment.BottomCenter
    else -> Alignment.Center
}
