package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.data.remoto.BusesPublicosDto
import co.urbanmovil.app.data.remoto.LugaresDto
import co.urbanmovil.app.data.remoto.PuntoDto
import co.urbanmovil.app.data.remoto.RutaPublicaDto
import co.urbanmovil.app.data.remoto.UrbanMovilApi
import co.urbanmovil.app.domain.calcularVigencia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
private fun relojDelSistema(): Long = Clock.System.now().toEpochMilliseconds()

@OptIn(ExperimentalTime::class)
private fun epochMs(iso: String): Long = Instant.parse(iso).toEpochMilliseconds()

/**
 * Datos reales del backend: rutas y recorridos activos de `/api/v1/public/...`.
 *
 * El servidor NO se consulta en cada cuadro: cada [intervaloSincronizacion] se pregunta qué
 * recorridos hay (y con qué `inicio`), y la posición se calcula aquí con [SimuladorRecorrido],
 * la misma fórmula del backend y de la web. Por eso los tres ven el bus en el mismo lugar.
 *
 * Lugares y «mi ubicación» también vienen del backend: son los de su ciudad ficticia.
 *
 * @param reloj hora LOCAL del teléfono (epoch ms); inyectable para pruebas.
 * @param sincronizarEnSegundoPlano `false` en pruebas: solo se sincroniza al llamar [sincronizar].
 */
class RemoteTransitRepository(
    private val api: UrbanMovilApi,
    private val reloj: () -> Long = ::relojDelSistema,
    private val intervaloEmision: Duration = 1.seconds,
    private val intervaloSincronizacion: Duration = 10.seconds,
    private val sincronizarEnSegundoPlano: Boolean = true,
    private val alcance: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : TransitRepository {

    private class Recorrido(val bus: Bus, val inicioMs: Long)

    private class Datos(
        val rutas: List<Ruta>,
        val lugares: List<Lugar>,
        val miUbicacion: Lugar,
        val recorridos: List<Recorrido>,
        val velocidadKmh: Double,
        /** Hora del servidor − hora del teléfono, en ms. */
        val desfaseMs: Long,
        /** Hora (del servidor) de la última sincronización correcta. */
        val sincronizadoEnMs: Long,
    )

    private val datos = MutableStateFlow<Datos?>(null)
    private var sincronizador: Job? = null

    private val _ajusteRelojMs = MutableStateFlow(0L)
    override val ajusteRelojMs: StateFlow<Long> = _ajusteRelojMs.asStateFlow()

    override suspend fun cargar() {
        sincronizar()
        if (sincronizarEnSegundoPlano && sincronizador?.isActive != true) {
            sincronizador = alcance.launch {
                while (isActive) {
                    delay(intervaloSincronizacion)
                    // Sin red se conservan los últimos datos; la vigencia avisa que envejecen.
                    try {
                        sincronizar()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    /** Pide rutas y buses al backend. Si falla, lanza y deja los datos anteriores intactos. */
    suspend fun sincronizar() = coroutineScope {
        val rutas = async { api.rutas() }
        val buses = async { api.buses() }
        val lugares = async { api.lugares() }
        datos.value = construir(rutas.await(), buses.await(), lugares.await(), recibidoEnMs = reloj())
    }

    override fun rutas(): List<Ruta> = datos.value?.rutas.orEmpty()
    override fun ruta(id: String): Ruta? = rutas().firstOrNull { it.id == id }
    override fun buses(): List<Bus> = datos.value?.recorridos.orEmpty().map { it.bus }
    override fun lugares(): List<Lugar> = datos.value?.lugares.orEmpty()

    /** Antes de [cargar] no hay datos: se usa la de [FakeData] para no devolver null a la UI. */
    override fun miUbicacion(): Lugar = datos.value?.miUbicacion ?: FakeData.MI_UBICACION

    override fun ajustarReloj(ajusteMs: Long) {
        _ajusteRelojMs.value = ajusteMs
    }

    override fun ahoraMs(): Long = reloj() + (datos.value?.desfaseMs ?: 0L) + _ajusteRelojMs.value

    override fun estadosEn(ahoraMs: Long): List<EstadoBus> {
        val d = datos.value ?: return emptyList()
        val rutasPorId = d.rutas.associateBy { it.id }
        return d.recorridos.mapNotNull { recorrido ->
            val ruta = rutasPorId[recorrido.bus.rutaId] ?: return@mapNotNull null
            if (ruta.principal.isEmpty()) return@mapNotNull null
            val posicion = SimuladorRecorrido.posicion(ruta.principal, recorrido.inicioMs, ahoraMs, d.velocidadKmh)
            EstadoBus(
                bus = recorrido.bus,
                posicion = posicion.punto,
                posicionEnRecorridoM = posicion.metros,
                // Lo último que confirmó el servidor: si se corta la red, el bus pasa a «Desactualizado».
                horaUltimaUbicacion = d.sincronizadoEnMs,
                vigencia = calcularVigencia(ahoraMs - d.sincronizadoEnMs),
            )
        }
    }

    override fun estadosBuses(): Flow<List<EstadoBus>> = flow {
        while (true) {
            emit(estadosEn(ahoraMs()))
            delay(intervaloEmision)
        }
    }.flowOn(Dispatchers.Default)

    private fun construir(rutas: List<RutaPublicaDto>, buses: BusesPublicosDto, lugares: LugaresDto, recibidoEnMs: Long): Datos {
        val generadoEnMs = epochMs(buses.generadoEn)
        return Datos(
            rutas = rutas.map { aRuta(it, buses.velocidadKmh) },
            lugares = lugares.lugares.map { Lugar(it.nombre, aPunto(it.punto)) },
            miUbicacion = lugares.ubicacionPasajero.let { Lugar(it.nombre, aPunto(it.punto)) },
            recorridos = buses.buses.map { dto ->
                Recorrido(
                    // Bus.id es el número interno («UM-104»): un vehículo está en un solo recorrido activo.
                    bus = Bus(id = dto.numeroInterno, rutaId = dto.rutaCodigo, conductor = "", desfaseMin = 0),
                    inicioMs = epochMs(dto.inicio),
                )
            },
            velocidadKmh = buses.velocidadKmh,
            desfaseMs = generadoEnMs - recibidoEnMs,
            sincronizadoEnMs = generadoEnMs,
        )
    }

    private fun aRuta(dto: RutaPublicaDto, velocidadKmh: Double): Ruta {
        val principal = dto.recorridoPrincipal.map(::aPunto)
        return Ruta(
            id = dto.codigo,
            nombre = dto.nombre,
            colorHex = colorArgb(dto.color),
            principal = principal,
            alternos = dto.alternos.map { alterno -> alterno.puntos.map(::aPunto) },
            minutosPorVuelta = SimuladorRecorrido.minutosPorVuelta(principal, velocidadKmh),
        )
    }

    private fun aPunto(p: PuntoDto) = Punto(p.lat, p.lng)

    private companion object {
        private const val GRIS = 0xFF6B7280

        /** `#RRGGBB` → ARGB opaco. Un color mal formado no tumba la app: se pinta gris. */
        fun colorArgb(hex: String): Long {
            val rgb = hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16) ?: return GRIS
            return 0xFF000000 or rgb
        }
    }
}
