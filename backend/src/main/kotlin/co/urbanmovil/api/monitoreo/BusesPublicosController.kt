package co.urbanmovil.api.monitoreo

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Sin autenticación (todo `/api/v1/public/` es público): buses en recorrido para la app del pasajero. */
@RestController
@Tag(name = "Público", description = "Endpoints sin autenticación para la app del pasajero")
class BusesPublicosController(private val monitoreoService: MonitoreoService) {

    @GetMapping("/api/v1/public/buses")
    @SecurityRequirements
    @Operation(
        summary = "Buses en recorrido (pasajero)",
        description = "Posición SIMULADA, igual a la del panel. Con `inicio`, `velocidadKmh` y `generadoEn` " +
            "el cliente calcula la posición entre consultas y corrige el desfase de su reloj.",
    )
    fun buses(): BusesPublicosResponse = monitoreoService.busesPublicos()
}
