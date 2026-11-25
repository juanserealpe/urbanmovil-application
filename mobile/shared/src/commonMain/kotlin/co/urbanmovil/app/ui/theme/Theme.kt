package co.urbanmovil.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val EsquemaClaro = lightColorScheme(
    primary = UrbanColores.Primario,
    onPrimary = Color.White,
    primaryContainer = UrbanColores.PrimarioContenedor,
    onPrimaryContainer = UrbanColores.Primario,
    secondaryContainer = UrbanColores.PrimarioContenedor,
    onSecondaryContainer = UrbanColores.Primario,
    tertiary = UrbanColores.CaminoAlterno,
    background = UrbanColores.Fondo,
    onBackground = UrbanColores.Texto,
    surface = Color.White,
    onSurface = UrbanColores.Texto,
    surfaceVariant = UrbanColores.Fondo,
    onSurfaceVariant = UrbanColores.TextoSecundario,
    surfaceContainer = Color.White,
    outline = UrbanColores.Borde,
    outlineVariant = UrbanColores.Borde,
)

/**
 * Tipografía: por ahora la del sistema. Para usar Inter (cuerpo) y Space Grotesk (títulos), copiar los
 * `.ttf` a `composeResources/font/` y reemplazar estas familias por `FontFamily(Font(Res.font.xxx))`.
 */
private val FamiliaTitulos: FontFamily = FontFamily.Default
private val FamiliaCuerpo: FontFamily = FontFamily.Default

private val TipografiaUrban: Typography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontFamily = FamiliaTitulos, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = FamiliaTitulos, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = FamiliaTitulos, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = FamiliaTitulos, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = FamiliaTitulos, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = FamiliaCuerpo),
        bodyMedium = bodyMedium.copy(fontFamily = FamiliaCuerpo),
        bodySmall = bodySmall.copy(fontFamily = FamiliaCuerpo),
        labelLarge = labelLarge.copy(fontFamily = FamiliaCuerpo, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = FamiliaCuerpo),
    )
}

@Composable
fun UrbanMovilTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaClaro, typography = TipografiaUrban, content = content)
}
