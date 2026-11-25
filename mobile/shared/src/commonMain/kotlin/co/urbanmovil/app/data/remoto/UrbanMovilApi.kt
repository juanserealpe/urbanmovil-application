package co.urbanmovil.app.data.remoto

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------- Contrato con el backend (endpoints públicos, sin sesión) ----------

@Serializable
data class PuntoDto(val lat: Double, val lng: Double)

@Serializable
data class AlternoDto(val nombre: String, val puntos: List<PuntoDto>)

/** `GET /api/v1/public/rutas` */
@Serializable
data class RutaPublicaDto(
    val codigo: String,
    val nombre: String,
    /** `#RRGGBB` */
    val color: String,
    val recorridoPrincipal: List<PuntoDto>,
    val alternos: List<AlternoDto> = emptyList(),
)

@Serializable
data class BusPublicoDto(
    val recorridoId: Long,
    val numeroInterno: String,
    val rutaCodigo: String,
    /** ISO-8601 con zona, p. ej. `2026-11-24T10:00:00-05:00`. */
    val inicio: String,
    val posicion: PuntoDto,
)

/** `GET /api/v1/public/buses` */
@Serializable
data class BusesPublicosDto(
    val simulado: Boolean,
    /** Hora del servidor al responder: sirve para corregir el reloj del teléfono. */
    val generadoEn: String,
    val velocidadKmh: Double,
    val buses: List<BusPublicoDto>,
)

@Serializable
data class LugarDto(val nombre: String, val punto: PuntoDto)

/** `GET /api/v1/public/mapa/lugares`: lugares de la ciudad ficticia del demo. */
@Serializable
data class LugaresDto(val ubicacionPasajero: LugarDto, val lugares: List<LugarDto>)

/**
 * Cliente de los endpoints públicos del backend. Es el ÚNICO lugar que conoce Ktor.
 * @param urlBase p. ej. `http://10.0.2.2:8080` (el «localhost» del PC visto desde el emulador).
 */
class UrbanMovilApi(urlBase: String, private val http: HttpClient = clienteHttp()) {

    private val base = urlBase.trimEnd('/')

    suspend fun rutas(): List<RutaPublicaDto> = http.get("$base/api/v1/public/rutas").body()

    suspend fun buses(): BusesPublicosDto = http.get("$base/api/v1/public/buses").body()

    suspend fun lugares(): LugaresDto = http.get("$base/api/v1/public/mapa/lugares").body()

    /** Estilo MapLibre de la ciudad ficticia: el mismo que usa la web. */
    val urlEstiloMapa: String = "$base/api/v1/public/mapa/estilo"

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Sin [motor], Ktor usa el que encuentre en el classpath (OkHttp en Android). */
        fun clienteHttp(motor: HttpClientEngine? = null): HttpClient {
            val configuracion: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
                // Un 4xx/5xx es un error: nunca se interpreta su cuerpo como datos.
                expectSuccess = true
                install(ContentNegotiation) { json(json) }
                install(HttpTimeout) {
                    connectTimeoutMillis = 5_000
                    requestTimeoutMillis = 10_000
                }
            }
            return if (motor != null) HttpClient(motor, configuracion) else HttpClient(configuracion)
        }
    }
}
