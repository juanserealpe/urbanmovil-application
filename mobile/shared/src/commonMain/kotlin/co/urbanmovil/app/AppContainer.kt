package co.urbanmovil.app

import androidx.compose.runtime.staticCompositionLocalOf
import co.urbanmovil.app.data.AuthRepository
import co.urbanmovil.app.data.FakeAuthRepository
import co.urbanmovil.app.data.FakeTransitRepository
import co.urbanmovil.app.data.RemoteTransitRepository
import co.urbanmovil.app.data.TransitRepository
import co.urbanmovil.app.data.remoto.UrbanMovilApi

/**
 * Inyección manual de dependencias. Para pasar a datos reales basta con cambiar las implementaciones aquí.
 */
class AppContainer(
    val transitRepository: TransitRepository = FakeTransitRepository(),
    val authRepository: AuthRepository = FakeAuthRepository(),
    /** Estilo del mapa. `null` = el de OpenFreeMap (Popayán real), para el modo sin backend. */
    val urlEstiloMapa: String? = null,
) {
    companion object {
        private var unico: AppContainer? = null

        /**
         * Una instancia por proceso: sobrevive a rotaciones y recreaciones de la Activity.
         * @param urlApi backend de UrbanMovil (p. ej. `http://10.0.2.2:8080`). Vacía = datos quemados,
         * el prototipo sin servidor de la Fase 1.
         */
        fun para(urlApi: String): AppContainer = unico ?: crear(urlApi).also { unico = it }

        private fun crear(urlApi: String): AppContainer {
            if (urlApi.isBlank()) return AppContainer()
            val api = UrbanMovilApi(urlApi)
            return AppContainer(transitRepository = RemoteTransitRepository(api), urlEstiloMapa = api.urlEstiloMapa)
        }

        /** Sin backend (previews y pruebas). */
        val instancia: AppContainer by lazy { AppContainer() }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer no provisto. Envuelve la UI en App().")
}

/** URL del estilo del mapa (la ciudad ficticia del backend), o `null` para el de OpenFreeMap. */
val LocalEstiloMapa = staticCompositionLocalOf<String?> { null }

/** `BuildConfig.DEBUG` de `androidApp`, disponible para la UI compartida (controles debug de la Fase 2). */
val LocalEsDebug = staticCompositionLocalOf { false }
