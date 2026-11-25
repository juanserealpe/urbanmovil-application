package co.urbanmovil.api.ruta

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as CuerpoDoc

private const val EJEMPLO_CREAR = """
{
  "codigo": "R9",
  "nombre": "Centro – Hospital San José",
  "color": "#0057B8",
  "recorridoPrincipal": [
    { "lat": 2.4419, "lng": -76.6066 },
    { "lat": 2.4436, "lng": -76.6049 },
    { "lat": 2.4462, "lng": -76.6031 }
  ],
  "alternos": [
    {
      "nombre": "Desvío por Cra 9",
      "puntos": [
        { "lat": 2.4419, "lng": -76.6066 },
        { "lat": 2.4440, "lng": -76.6085 },
        { "lat": 2.4462, "lng": -76.6031 }
      ]
    }
  ]
}
"""

private const val EJEMPLO_MODIFICAR = """
{
  "codigo": "R9",
  "nombre": "Centro – Hospital San José",
  "color": "#0057B8",
  "recorridoPrincipal": [
    { "lat": 2.4419, "lng": -76.6066 },
    { "lat": 2.4436, "lng": -76.6049 },
    { "lat": 2.4462, "lng": -76.6031 },
    { "lat": 2.4480, "lng": -76.6012 }
  ],
  "alternos": [
    {
      "id": 1,
      "nombre": "Desvío por Cra 9",
      "puntos": [
        { "lat": 2.4419, "lng": -76.6066 },
        { "lat": 2.4440, "lng": -76.6085 },
        { "lat": 2.4480, "lng": -76.6012 }
      ]
    }
  ]
}
"""

@RestController
@RequestMapping("/api/v1/rutas")
@Tag(name = "Rutas", description = "Rutas con recorrido principal y caminos alternos (HU-02, HU-03, HU-04, HU-05)")
class RutaController(private val rutaService: RutaService) {

    @GetMapping
    @Operation(summary = "Listar rutas (resumen)", description = "q busca en código o nombre")
    fun listar(@RequestParam(required = false) q: String?): List<RutaResumenResponse> = rutaService.listar(q)

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una ruta con su trazado y alternos")
    fun obtener(@PathVariable id: Long): RutaDetalleResponse = rutaService.obtener(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Registrar ruta",
        description = "El recorrido principal necesita 2 puntos o más, en el sentido de circulación. " +
            "Si no llega color se asigna uno de la paleta.",
        requestBody = CuerpoDoc(
            content = [Content(examples = [ExampleObject(name = "Ruta con un alterno", value = EJEMPLO_CREAR)])],
        ),
    )
    fun registrar(@Valid @RequestBody solicitud: RutaRequest): RutaDetalleResponse = rutaService.registrar(solicitud)

    @PutMapping("/{id}")
    @Operation(
        summary = "Modificar ruta (reemplazo completo)",
        description = "Reemplaza código, nombre, color, recorrido y la lista completa de alternos: " +
            "un alterno con id se actualiza, uno sin id se crea y uno que no viene se elimina.",
        requestBody = CuerpoDoc(
            content = [Content(examples = [ExampleObject(name = "Cambiar trazado y conservar un alterno", value = EJEMPLO_MODIFICAR)])],
        ),
    )
    fun modificar(@PathVariable id: Long, @Valid @RequestBody solicitud: RutaRequest): RutaDetalleResponse =
        rutaService.modificar(id, solicitud)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar ruta", description = "No se puede si tiene recorridos activos")
    fun eliminar(@PathVariable id: Long) = rutaService.eliminar(id)
}
