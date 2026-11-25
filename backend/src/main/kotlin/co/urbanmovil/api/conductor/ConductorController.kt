package co.urbanmovil.api.conductor

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/conductores")
@Tag(name = "Conductores", description = "Registro, modificación, eliminación y disponibilidad (HU-06, HU-07, HU-08)")
class ConductorController(private val conductorService: ConductorService) {

    @GetMapping
    @Operation(summary = "Listar conductores", description = "q busca en nombre, identificación o correo")
    fun listar(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) disponible: Boolean?,
    ): List<ConductorResponse> = conductorService.listar(q, disponible)

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un conductor")
    fun obtener(@PathVariable id: Long): ConductorResponse = conductorService.obtener(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar conductor")
    fun registrar(@Valid @RequestBody solicitud: ConductorCrearRequest): ConductorResponse =
        conductorService.registrar(solicitud)

    @PutMapping("/{id}")
    @Operation(summary = "Modificar datos del conductor", description = "No cambia la disponibilidad")
    fun modificar(@PathVariable id: Long, @Valid @RequestBody solicitud: ConductorModificarRequest): ConductorResponse =
        conductorService.modificar(id, solicitud)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar conductor definitivamente")
    fun eliminar(@PathVariable id: Long) = conductorService.eliminar(id)

    @PatchMapping("/{id}/disponibilidad")
    @Operation(summary = "Marcar disponible o no disponible")
    fun cambiarDisponibilidad(
        @PathVariable id: Long,
        @Valid @RequestBody solicitud: DisponibilidadRequest,
    ): ConductorResponse = conductorService.cambiarDisponibilidad(id, solicitud)
}
