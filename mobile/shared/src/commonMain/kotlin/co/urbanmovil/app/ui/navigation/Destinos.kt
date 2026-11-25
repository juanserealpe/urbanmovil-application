package co.urbanmovil.app.ui.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.ic_bus
import urbanmovil.shared.generated.resources.ic_buscar
import urbanmovil.shared.generated.resources.ic_perfil
import urbanmovil.shared.generated.resources.ic_reloj
import urbanmovil.shared.generated.resources.tab_buscar
import urbanmovil.shared.generated.resources.tab_cuando_pasa
import urbanmovil.shared.generated.resources.tab_perfil
import urbanmovil.shared.generated.resources.tab_rutas

/** Pestañas de la barra inferior. */
enum class Pestana(val ruta: String, val titulo: StringResource, val icono: DrawableResource) {
    Rutas("rutas", Res.string.tab_rutas, Res.drawable.ic_bus),
    CuandoPasa("cuandoPasa", Res.string.tab_cuando_pasa, Res.drawable.ic_reloj),
    BuscarViaje("buscarViaje", Res.string.tab_buscar, Res.drawable.ic_buscar),
    Perfil("perfil", Res.string.tab_perfil, Res.drawable.ic_perfil),
}

/** Destinos sin barra inferior: detalle de ruta (Fase 2) y guía (Fase 3). */
object Destinos {
    const val ARG_RUTA_ID = "rutaId"
    const val ARG_OPCION_ID = "opcionId"

    const val DETALLE_RUTA = "detalleRuta/{$ARG_RUTA_ID}"
    const val GUIA = "guia/{$ARG_OPCION_ID}"

    fun detalleRuta(rutaId: String) = "detalleRuta/$rutaId"
    fun guia(opcionId: String) = "guia/$opcionId"
}
