package co.urbanmovil.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.urbanmovil.app.ui.login.LoginScreen
import co.urbanmovil.app.ui.navigation.NavegacionPrincipal
import co.urbanmovil.app.ui.theme.UrbanMovilTheme

/**
 * Raíz de la UI compartida.
 * @param esDebug `BuildConfig.DEBUG` de `androidApp` (habilita controles de depuración en fases siguientes).
 */
@Composable
fun App(esDebug: Boolean, contenedor: AppContainer = AppContainer.instancia) {
    CompositionLocalProvider(
        LocalAppContainer provides contenedor,
        LocalEsDebug provides esDebug,
    ) {
        UrbanMovilTheme {
            val sesion by contenedor.authRepository.sesion.collectAsStateWithLifecycle()
            // RN-18: sin sesión solo existe la pantalla de acceso. Al cerrar sesión se descarta
            // toda la navegación y al volver a entrar se arranca de cero en Rutas.
            if (sesion == null) LoginScreen() else NavegacionPrincipal()
        }
    }
}
