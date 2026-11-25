package co.urbanmovil.api.vehiculo

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
@RequestMapping("/api/v1/vehiculos")
@Tag(name = "Vehículos", description = "Registro, modificación, eliminación y estado (HU-28, HU-29, HU-30)")
class VehiculoController(private val vehiculoService: VehiculoService) {

    @GetMapping
    @Operation(summary = "Listar vehículos", description = "q busca en placa o número interno")
    fun listar(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) activo: Boolean?,
    ): List<VehiculoResponse> = vehiculoService.listar(q, activo)

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un vehículo")
    fun obtener(@PathVariable id: Long): VehiculoResponse = vehiculoService.obtener(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar vehículo", description = "La placa se guarda en mayúsculas, sin espacios ni guiones")
    fun registrar(@Valid @RequestBody solicitud: VehiculoRequest): VehiculoResponse =
        vehiculoService.registrar(solicitud)

    @PutMapping("/{id}")
    @Operation(summary = "Modificar placa y número interno")
    fun modificar(@PathVariable id: Long, @Valid @RequestBody solicitud: VehiculoRequest): VehiculoResponse =
        vehiculoService.modificar(id, solicitud)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar vehículo definitivamente")
    fun eliminar(@PathVariable id: Long) = vehiculoService.eliminar(id)

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Desactivar (con motivo) o reactivar")
    fun cambiarEstado(
        @PathVariable id: Long,
        @Valid @RequestBody solicitud: EstadoVehiculoRequest,
    ): VehiculoResponse = vehiculoService.cambiarEstado(id, solicitud)
}
