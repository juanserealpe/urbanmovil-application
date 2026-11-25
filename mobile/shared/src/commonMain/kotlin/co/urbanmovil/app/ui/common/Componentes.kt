package co.urbanmovil.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.urbanmovil.app.ui.theme.UrbanColores
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.ic_bus
import urbanmovil.shared.generated.resources.logo_descripcion

/** Franja de aviso naranja (modo demo, ocupación). */
@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = UrbanColores.AvisoFondo,
        contentColor = UrbanColores.AvisoTexto,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** Logo: bus blanco sobre círculo primario. */
@Composable
fun LogoUrbanMovil(modifier: Modifier = Modifier, tamano: Dp = 88.dp) {
    Surface(
        modifier = modifier.size(tamano),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.ic_bus),
                contentDescription = stringResource(Res.string.logo_descripcion),
                tint = Color.White,
                modifier = Modifier.size(tamano * 0.5f),
            )
        }
    }
}
