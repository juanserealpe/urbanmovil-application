package co.urbanmovil.app.data.model

data class Ruta(
    /** Identificador corto, p. ej. "R1". */
    val id: String,
    /** Nombre visible, p. ej. "Centro – Campanario". */
    val nombre: String,
    /** Color ARGB de la ruta en el mapa, p. ej. 0xFF1F4FD8. */
    val colorHex: Long,
    /** Recorrido principal, ordenado en el sentido de circulación (inicio → final). */
    val principal: List<Punto>,
    /** Caminos alternos. Solo informativos: NUNCA se usan para ETA ni búsqueda (RN-03). */
    val alternos: List<List<Punto>> = emptyList(),
    /** Duración de un recorrido completo, usada por la simulación. */
    val minutosPorVuelta: Int,
)
