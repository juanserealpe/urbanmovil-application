package co.urbanmovil.api.ruta

/**
 * Punto de un trazado, en el sentido de circulación: `{"lat": 2.4419, "lng": -76.6066}`.
 *
 * Los valores por defecto generan un constructor sin argumentos: así Hibernate puede
 * leer el JSONB con su FormatMapper (que no trae el módulo de Kotlin para Jackson).
 */
data class Punto(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
)
