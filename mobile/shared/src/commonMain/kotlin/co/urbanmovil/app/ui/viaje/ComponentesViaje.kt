package co.urbanmovil.app.ui.viaje

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.buscar_mi_ubicacion
import urbanmovil.shared.generated.resources.buscar_punto_mapa

/** Estilos de chip usados en tarjetas y guía. Todos cumplen contraste 4.5:1. */
enum class EstiloChip(val fondo: Color, val texto: Color) {
    Destacado(UrbanColores.ExitoFondo, UrbanColores.ExitoTexto),
    Tiempo(UrbanColores.PrimarioContenedor, UrbanColores.Primario),
    Neutro(Color(0xFFF1F2F4), UrbanColores.Desactualizado),
}

@Composable
internal fun Chip(texto: String, estilo: EstiloChip, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = estilo.texto,
        modifier = modifier
            .background(estilo.fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Nombre visible de un origen o destino. */
@Composable
internal fun nombrePunto(punto: PuntoElegido): String = when (punto) {
    is PuntoElegido.MiUbicacion -> stringResource(Res.string.buscar_mi_ubicacion)
    is PuntoElegido.DeLugar -> punto.lugar.nombre
    is PuntoElegido.EnMapa -> stringResource(Res.string.buscar_punto_mapa)
}
