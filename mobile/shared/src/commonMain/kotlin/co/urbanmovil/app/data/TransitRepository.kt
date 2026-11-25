package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.Ruta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Única puerta de la UI a los datos de transporte. Hoy la implementa [FakeTransitRepository];
 * mañana una implementación real, sin tocar pantallas ni ViewModels.
 */
interface TransitRepository {
    /**
     * Trae los datos iniciales. La UI la llama (y espera) antes de mostrar las pantallas, así el
     * resto de métodos pueden seguir siendo síncronos. Lanza si no se pudo cargar.
     * Los datos quemados no necesitan cargarse.
     */
    suspend fun cargar() {}

    fun rutas(): List<Ruta>
    fun ruta(id: String): Ruta?
    fun buses(): List<Bus>
    fun lugares(): List<Lugar>

    /** Ubicación del pasajero. En el MVP es un punto fijo (sin GPS). */
    fun miUbicacion(): Lugar

    /** Desfase aplicado al reloj del teléfono (control debug «adelantar el tiempo», Fase 2). */
    val ajusteRelojMs: StateFlow<Long>
    fun ajustarReloj(ajusteMs: Long)

    /** Hora actual de la simulación (reloj del teléfono + [ajusteRelojMs]), en epoch millis. */
    fun ahoraMs(): Long

    /** Estado de todos los buses en [ahoraMs]. */
    fun estadosEn(ahoraMs: Long): List<EstadoBus>

    /** Emite el estado de todos los buses cada segundo. */
    fun estadosBuses(): Flow<List<EstadoBus>>
}
