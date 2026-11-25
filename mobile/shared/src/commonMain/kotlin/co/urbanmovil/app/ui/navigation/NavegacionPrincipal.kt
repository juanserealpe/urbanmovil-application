package co.urbanmovil.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.urbanmovil.app.ui.profile.ProfileScreen
import co.urbanmovil.app.ui.rutas.DetalleRutaScreen
import co.urbanmovil.app.ui.rutas.RutasScreen
import co.urbanmovil.app.ui.viaje.BuscarViajeScreen
import co.urbanmovil.app.ui.viaje.BuscarViajeViewModel
import co.urbanmovil.app.ui.viaje.CuandoPasaScreen
import co.urbanmovil.app.ui.viaje.GuiaScreen
import co.urbanmovil.app.LocalAppContainer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Navegación con sesión iniciada. Solo se compone si hay sesión (RN-18), así que ningún destino
 * es alcanzable sin pasar por el login. El arranque siempre es la pestaña Rutas (CA-17.1).
 */
@Composable
fun NavegacionPrincipal(navController: NavHostController = rememberNavController()) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    val mostrarBarra = Pestana.entries.any { destinoActual.esDe(it) }

    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                BarraInferior(destinoActual) { pestana -> navController.irAPestana(pestana) }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Pestana.Rutas.ruta,
            modifier = Modifier.padding(padding),
        ) {
            composable(Pestana.Rutas.ruta) {
                RutasScreen(alAbrirRuta = { rutaId -> navController.navigate(Destinos.detalleRuta(rutaId)) })
            }
            composable(Pestana.CuandoPasa.ruta) { CuandoPasaScreen() }
            composable(Pestana.BuscarViaje.ruta) {
                BuscarViajeScreen(alAbrirGuia = { opcionId -> navController.navigate(Destinos.guia(opcionId)) })
            }
            composable(Pestana.Perfil.ruta) { ProfileScreen() }

            composable(
                route = Destinos.DETALLE_RUTA,
                arguments = listOf(navArgument(Destinos.ARG_RUTA_ID) { type = NavType.StringType }),
            ) { entrada ->
                val rutaId = entrada.savedStateHandle.get<String>(Destinos.ARG_RUTA_ID).orEmpty()
                DetalleRutaScreen(rutaId = rutaId, alVolver = { navController.popBackStack() })
            }
            composable(
                route = Destinos.GUIA,
                arguments = listOf(navArgument(Destinos.ARG_OPCION_ID) { type = NavType.StringType }),
            ) { entrada ->
                val opcionId = entrada.savedStateHandle.get<String>(Destinos.ARG_OPCION_ID).orEmpty()
                // La guía usa el MISMO ViewModel que la lista: se pide a la entrada de la pestaña
                // «Buscar viaje», que sigue debajo en el back stack (CA-23.2).
                val entradaBuscar = remember(entrada) {
                    runCatching { navController.getBackStackEntry(Pestana.BuscarViaje.ruta) }.getOrNull()
                }
                if (entradaBuscar == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    val repo = LocalAppContainer.current.transitRepository
                    val viewModel = viewModel(viewModelStoreOwner = entradaBuscar) { BuscarViajeViewModel(repo) }
                    GuiaScreen(opcionId = opcionId, viewModel = viewModel, alVolver = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun BarraInferior(destinoActual: NavDestination?, alElegir: (Pestana) -> Unit) {
    NavigationBar {
        Pestana.entries.forEach { pestana ->
            NavigationBarItem(
                selected = destinoActual.esDe(pestana),
                onClick = { alElegir(pestana) },
                // El texto de la etiqueta ya describe el botón; el ícono es decorativo.
                icon = { Icon(painterResource(pestana.icono), contentDescription = null) },
                label = { Text(stringResource(pestana.titulo)) },
            )
        }
    }
}

private fun NavDestination?.esDe(pestana: Pestana): Boolean =
    this?.hierarchy?.any { it.route == pestana.ruta } == true

private fun NavHostController.irAPestana(pestana: Pestana) {
    navigate(pestana.ruta) {
        // Una sola copia de cada pestaña y se conserva su estado al volver.
        popUpTo(Pestana.Rutas.ruta) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
