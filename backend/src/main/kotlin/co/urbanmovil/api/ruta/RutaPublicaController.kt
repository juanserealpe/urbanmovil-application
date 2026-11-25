package co.urbanmovil.api.ruta

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Sin autenticación: es lo que consumirá la app del pasajero (V-3). */
@RestController
@RequestMapping("/api/v1/public/rutas")
@Tag(name = "Público", description = "Endpoints sin autenticación para la app del pasajero")
class RutaPublicaController(private val rutaService: RutaService) {

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Rutas para pasajeros", description = "Ordenadas por código; refleja los cambios al instante")
    fun listar(): List<RutaPublicaResponse> = rutaService.listarPublicas()
}
