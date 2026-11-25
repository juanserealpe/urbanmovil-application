package co.urbanmovil.api.monitoreo

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Monitoreo", description = "Buses en recorrido con posición simulada (HU-31)")
class MonitoreoController(private val monitoreoService: MonitoreoService) {

    @GetMapping("/api/v1/monitoreo/buses")
    @Operation(
        summary = "Buses en recorrido",
        description = "Posición SIMULADA sobre el recorrido principal según la hora. El panel lo consulta cada 3–5 s.",
    )
    fun buses(): MonitoreoResponse = monitoreoService.buses()
}
