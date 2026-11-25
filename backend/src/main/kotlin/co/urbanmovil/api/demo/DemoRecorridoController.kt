package co.urbanmovil.api.demo

import co.urbanmovil.api.recorrido.EstadoRecorrido
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Sin el perfil demo este controlador no existe y las rutas responden 404. */
@RestController
@RequestMapping("/api/v1/demo/recorridos")
@ConditionalOnProperty(prefix = "urbanmovil.demo", name = ["habilitado"], havingValue = "true")
@Tag(name = "Demo · Recorridos", description = "D-16: iniciar y finalizar recorridos sin la app del conductor (solo perfil demo)")
class DemoRecorridoController(private val demoRecorridoService: DemoRecorridoService) {

    @GetMapping
    @Operation(summary = "Listar recorridos", description = "estado opcional: ACTIVO o FINALIZADO")
    fun listar(@RequestParam(required = false) estado: EstadoRecorrido?): List<RecorridoDemoResponse> =
        demoRecorridoService.listar(estado)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar recorrido", description = "La ruta debe estar en la programación de hoy del conductor")
    fun iniciar(@Valid @RequestBody solicitud: IniciarRecorridoRequest): RecorridoDemoResponse =
        demoRecorridoService.iniciar(solicitud)

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar recorrido")
    fun finalizar(@PathVariable id: Long): RecorridoDemoResponse = demoRecorridoService.finalizar(id)
}
