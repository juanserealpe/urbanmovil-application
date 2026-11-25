package co.urbanmovil.api.ruta

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

// ------------------------------------------------------------------ Entrada

/** Coordenadas nulas para poder responder «Coordenadas inválidas» en vez de un error de lectura del JSON. */
data class PuntoRequest(
    @field:Schema(example = "2.4419")
    val lat: Double?,

    @field:Schema(example = "-76.6066")
    val lng: Double?,
)

data class CaminoAlternoRequest(
    @field:Schema(description = "Si llega, se actualiza ese alterno; si no, se crea uno nuevo", example = "null", nullable = true)
    val id: Long? = null,

    @field:NotBlank(message = "Escriba el nombre del camino alterno")
    @field:Size(max = 120, message = "El nombre del camino alterno admite máximo 120 caracteres")
    @field:Schema(example = "Desvío por Cra 9")
    val nombre: String?,

    val puntos: List<PuntoRequest?>? = null,
)

/** Cuerpo de POST y PUT. En PUT reemplaza todo, incluida la lista completa de alternos. */
data class RutaRequest(
    @field:NotBlank(message = "Escriba el código de la ruta")
    @field:Size(max = 10, message = "El código admite máximo 10 caracteres")
    @field:Schema(example = "R1")
    val codigo: String?,

    @field:NotBlank(message = "Escriba el nombre de la ruta")
    @field:Size(max = 120, message = "El nombre admite máximo 120 caracteres")
    @field:Schema(example = "Centro – Campanario")
    val nombre: String?,

    @field:Pattern(regexp = "^(|#[0-9A-Fa-f]{6})$", message = "Color inválido")
    @field:Schema(description = "Opcional. Si no llega se asigna uno de la paleta", example = "#0057B8", nullable = true)
    val color: String? = null,

    @field:Schema(description = "Puntos en el sentido de circulación: el primero es el inicio y el último el final")
    val recorridoPrincipal: List<PuntoRequest?>? = null,

    @field:Valid
    val alternos: List<CaminoAlternoRequest>? = null,
)

// ------------------------------------------------------------------ Salida

data class RutaResumenResponse(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val color: String,
    val puntosRecorrido: Int,
    val numAlternos: Int,
    val recorridosActivos: Long,
    /** Nombres de los conductores que tienen la ruta en su programación de hoy. */
    val conductoresHoy: List<String>,
    val actualizadoEn: OffsetDateTime,
)

data class CaminoAlternoResponse(
    val id: Long,
    val nombre: String,
    val puntos: List<Punto>,
)

data class RutaDetalleResponse(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val color: String,
    val puntosRecorrido: Int,
    val numAlternos: Int,
    val recorridosActivos: Long,
    val conductoresHoy: List<String>,
    val actualizadoEn: OffsetDateTime,
    val recorridoPrincipal: List<Punto>,
    val inicio: Punto?,
    val final: Punto?,
    val alternos: List<CaminoAlternoResponse>,
)

data class CaminoAlternoPublicoResponse(
    val nombre: String,
    val puntos: List<Punto>,
)

/** Lo que verá la app del pasajero (V-3). */
data class RutaPublicaResponse(
    val codigo: String,
    val nombre: String,
    val color: String,
    val recorridoPrincipal: List<Punto>,
    val alternos: List<CaminoAlternoPublicoResponse>,
)
