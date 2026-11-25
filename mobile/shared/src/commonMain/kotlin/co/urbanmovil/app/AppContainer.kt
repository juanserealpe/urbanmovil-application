package co.urbanmovil.app

import androidx.compose.runtime.staticCompositionLocalOf
import co.urbanmovil.app.data.AuthRepository
import co.urbanmovil.app.data.FakeAuthRepository
import co.urbanmovil.app.data.FakeTransitRepository
import co.urbanmovil.app.data.TransitRepository

/**
 * Inyección manual de dependencias. Para pasar a datos reales basta con cambiar las implementaciones aquí.
 */
class AppContainer(
    val transitRepository: TransitRepository = FakeTransitRepository(),
    val authRepository: AuthRepository = FakeAuthRepository(),
) {
    companion object {
        /** Una instancia por proceso: sobrevive a rotaciones y recreaciones de la Activity. */
        val instancia: AppContainer by lazy { AppContainer() }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer no provisto. Envuelve la UI en App().")
}

/** `BuildConfig.DEBUG` de `androidApp`, disponible para la UI compartida (controles debug de la Fase 2). */
val LocalEsDebug = staticCompositionLocalOf { false }
