package co.urbanmovil.api.reasignacion

import co.urbanmovil.api.common.NoAutenticadoException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@Tag(name = "Reasignaciones", description = "Reasignación de rutas por eventualidad, con historial (HU-25)")
class ReasignacionController(private val reasignacionService: ReasignacionService) {

    @PostMapping("/api/v1/conductores/{id}/reasignaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Reasignar rutas del conductor (hoy)",
        description = "Las rutas a asignar van al final. Una ruta que se quita mientras se recorre queda en RETIRO_PENDIENTE.",
    )
    fun reasignar(
        @PathVariable id: Long,
        @Valid @RequestBody solicitud: ReasignacionRequest,
        autenticacion: Authentication?,
    ): ReasignacionResponse =
        reasignacionService.reasignar(id, solicitud, autenticacion?.name ?: throw NoAutenticadoException())

    @GetMapping("/api/v1/reasignaciones")
    @Operation(summary = "Historial de reasignaciones", description = "Más reciente primero; filtros opcionales")
    fun historial(
        @RequestParam(required = false) conductorId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?,
    ): List<ReasignacionResponse> = reasignacionService.historial(conductorId, fecha)
}
