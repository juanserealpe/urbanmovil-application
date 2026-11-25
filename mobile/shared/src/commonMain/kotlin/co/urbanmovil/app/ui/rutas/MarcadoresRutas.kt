package co.urbanmovil.app.ui.rutas

import androidx.compose.runtime.Composable
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.ui.map.MarcadorMapa
import co.urbanmovil.app.ui.map.TipoMarcador
import org.jetbrains.compose.resources.stringResource
import urbanmovil.shared.generated.resources.Res
import urbanmovil.shared.generated.resources.mapa_bus_desactualizado
import urbanmovil.shared.generated.resources.mapa_bus_reciente
import urbanmovil.shared.generated.resources.mapa_mi_ubicacion

/** Marcadores de bus (los OCULTOS ya no llegan aquí: [BusUi] nunca los representa). */
@Composable
internal fun marcadoresDeBuses(buses: List<BusUi>, alTocar: ((BusUi) -> Unit)? = null): List<MarcadorMapa> =
    buses.map { bus ->
        MarcadorMapa(
            id = "bus-${bus.id}",
            punto = bus.punto,
            tipo = TipoMarcador.Bus(colorRuta = colorDe(bus.colorRuta), desactualizado = bus.desactualizado),
            descripcion = stringResource(
                if (bus.desactualizado) Res.string.mapa_bus_desactualizado else Res.string.mapa_bus_reciente,
                bus.id,
            ),
            alTocar = alTocar?.let { accion -> { accion(bus) } },
        )
    }

@Composable
internal fun marcadorMiUbicacion(lugar: Lugar): MarcadorMapa = MarcadorMapa(
    id = "mi-ubicacion",
    punto = lugar.punto,
    tipo = TipoMarcador.MiUbicacion,
    descripcion = stringResource(Res.string.mapa_mi_ubicacion),
)
