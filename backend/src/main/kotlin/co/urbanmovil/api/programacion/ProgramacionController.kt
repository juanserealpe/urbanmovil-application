package co.urbanmovil.api.programacion

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@Tag(name = "Programación", description = "Rutas del día por conductor y su orden (HU-09)")
class ProgramacionController(private val programacionService: ProgramacionService) {

    @GetMapping("/api/v1/conductores/{id}/programacion")
    @Operation(summary = "Programación de un conductor", description = "fecha opcional (YYYY-MM-DD); por defecto hoy")
    fun obtener(
        @PathVariable id: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?,
    ): ProgramacionResponse = programacionService.obtener(id, fecha)

    @PutMapping("/api/v1/conductores/{id}/programacion")
    @Operation(
        summary = "Reemplazar la programación",
        description = "rutas = lista completa y ordenada de ids. No se puede retirar la ruta que el conductor está recorriendo.",
    )
    fun reemplazar(
        @PathVariable id: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?,
        @Valid @RequestBody solicitud: ProgramacionRequest,
    ): ProgramacionResponse = programacionService.reemplazar(id, fecha, solicitud.rutas!!)

    @GetMapping("/api/v1/programaciones")
    @Operation(summary = "Resumen del día de todos los conductores")
    fun resumen(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?,
    ): List<ProgramacionResumenResponse> = programacionService.resumenDelDia(fecha)
}
