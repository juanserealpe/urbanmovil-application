package co.urbanmovil.api.mapa

import co.urbanmovil.api.ruta.Punto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

data class LugarResponse(val nombre: String, val punto: Punto)

data class LugaresResponse(val ubicacionPasajero: LugarResponse, val lugares: List<LugarResponse>)

/** Sin autenticación: la ciudad ficticia que dibujan web y app. */
@RestController
@RequestMapping("/api/v1/public/mapa")
@Tag(name = "Público", description = "Endpoints sin autenticación para la app del pasajero")
class MapaPublicoController {

    // La ciudad no cambia mientras corre el servidor: se arma una sola vez.
    private val estilo = EstiloMapa.estilo()

    private val lugares = LugaresResponse(
        ubicacionPasajero = CiudadFicticia.ubicacionPasajero.let { LugarResponse(it.nombre, it.punto) },
        lugares = CiudadFicticia.lugares.map { LugarResponse(it.nombre, it.punto) },
    )

    @GetMapping("/estilo")
    @SecurityRequirements
    @Operation(summary = "Estilo del mapa", description = "Estilo MapLibre v8 con las calles de la ciudad ficticia embebidas (GeoJSON)")
    fun estilo(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10))).body(estilo)

    @GetMapping("/lugares")
    @SecurityRequirements
    @Operation(summary = "Lugares de la ciudad", description = "Para la búsqueda de viaje y la ubicación simulada del pasajero")
    fun lugares(): LugaresResponse = lugares
}
