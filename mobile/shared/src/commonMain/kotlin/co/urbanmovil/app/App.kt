package co.urbanmovil.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.urbanmovil.app.data.TransitRepository
import co.urbanmovil.app.ui.login.LoginScreen
import co.urbanmovil.app.ui.navigation.NavegacionPrincipal
import co.urbanmovil.app.ui.theme.UrbanMovilTheme
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.carga_cargando
import urbanmovil.shared.generated.resources.carga_error
import urbanmovil.shared.generated.resources.carga_reintentar

/**
 * Raíz de la UI compartida.
 * @param esDebug `BuildConfig.DEBUG` de `androidApp` (habilita controles de depuración en fases siguientes).
 */
@Composable
fun App(esDebug: Boolean, contenedor: AppContainer = AppContainer.instancia) {
    CompositionLocalProvider(
        LocalAppContainer provides contenedor,
        LocalEsDebug provides esDebug,
        LocalEstiloMapa provides contenedor.urlEstiloMapa,
    ) {
        UrbanMovilTheme {
            val sesion by contenedor.authRepository.sesion.collectAsStateWithLifecycle()
            // RN-18: sin sesión solo existe la pantalla de acceso. Al cerrar sesión se descarta
            // toda la navegación y al volver a entrar se arranca de cero en Rutas.
            if (sesion == null) LoginScreen() else ConDatos(contenedor.transitRepository) { NavegacionPrincipal() }
        }
    }
}

private enum class EstadoCarga { CARGANDO, LISTO, ERROR }

/** Muestra [contenido] solo cuando el repositorio ya tiene rutas y buses (los ViewModels los leen al crearse). */
@Composable
private fun ConDatos(repo: TransitRepository, contenido: @Composable () -> Unit) {
    var estado by remember { mutableStateOf(EstadoCarga.CARGANDO) }
    var intento by remember { mutableIntStateOf(0) }

    LaunchedEffect(intento) {
        estado = EstadoCarga.CARGANDO
        estado = try {
            repo.cargar()
            EstadoCarga.LISTO
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            EstadoCarga.ERROR
        }
    }

    if (estado == EstadoCarga.LISTO) {
        contenido()
        return
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (estado == EstadoCarga.CARGANDO) {
                CircularProgressIndicator()
                Text(stringResource(Res.string.carga_cargando), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    stringResource(Res.string.carga_error),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { intento++ }) { Text(stringResource(Res.string.carga_reintentar)) }
            }
        }
    }
}
