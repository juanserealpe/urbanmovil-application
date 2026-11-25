package co.urbanmovil.app.data.model

/** Vigencia de la última ubicación conocida de un bus (RN-07). */
enum class Vigencia { RECIENTE, DESACTUALIZADA, OCULTA }

/** Foto del bus en un instante dado. */
data class EstadoBus(
    val bus: Bus,
    val posicion: Punto,
    /**
     * Posición sobre el recorrido principal, en metros acumulados desde el inicio.
     * (El documento lo llama `indiceEnRecorrido`; se usa metros porque es lo que necesitan ETA y búsqueda.)
     */
    val posicionEnRecorridoM: Double,
    /** Epoch millis de la última ubicación reportada. */
    val horaUltimaUbicacion: Long,
    val vigencia: Vigencia,
)
