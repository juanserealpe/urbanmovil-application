package co.urbanmovil.api.demo

import co.urbanmovil.api.ruta.Punto
import kotlin.math.round

/**
 * Trazados de demostración sobre Popayán (alrededor del Parque Caldas, 2.4419, -76.6066).
 *
 * Son COORDENADAS APROXIMADAS: no existe todavía el FakeData.kt de la app del pasajero para
 * reutilizarlas. Para reemplazarlas:
 *   1. Dibuje la ruta como LineString en https://geojson.io, en el sentido de circulación.
 *   2. Copie el arreglo `coordinates` (viene como [lng, lat]) e inviértalo a `p(lat, lng)`.
 *   3. Pegue los puntos en la lista correspondiente (puede quitar `densificar`).
 * Si después aparece el FakeData.kt, copie aquí exactamente sus coordenadas para que ambas apps coincidan.
 */
object RutasDemo {

    data class AlternoDemo(val nombre: String, val puntos: List<Punto>)

    data class RutaDemo(
        val codigo: String,
        val nombre: String,
        val color: String,
        val recorrido: List<Punto>,
        val alternos: List<AlternoDemo> = emptyList(),
    )

    private fun p(lat: Double, lng: Double) = Punto(lat, lng)

    /** Inserta puntos intermedios entre cada par de esquinas (trazado más suave en el mapa). */
    private fun densificar(esquinas: List<Punto>, intermedios: Int = 1): List<Punto> =
        esquinas.zipWithNext().flatMap { (a, b) ->
            (0..intermedios).map { i ->
                val t = i.toDouble() / (intermedios + 1)
                Punto(redondear(a.lat + (b.lat - a.lat) * t), redondear(a.lng + (b.lng - a.lng) * t))
            }
        } + esquinas.last()

    private fun redondear(valor: Double) = round(valor * 1_000_000) / 1_000_000

    /** R1 · Parque Caldas → hacia el norte por la Cra 6 / Av. Panamericana → Campanario. 21 puntos. */
    private val r1 = densificar(
        listOf(
            p(2.44190, -76.60660), p(2.44320, -76.60600), p(2.44460, -76.60580), p(2.44610, -76.60470),
            p(2.44780, -76.60330), p(2.44950, -76.60180), p(2.45130, -76.60020), p(2.45320, -76.59870),
            p(2.45510, -76.59740), p(2.45690, -76.59620), p(2.45860, -76.59560),
        ),
    )

    /** Desvío por Cra 9: sale del centro hacia el occidente y se reincorpora más al norte. */
    private val r1Desvio = densificar(
        listOf(
            p(2.44190, -76.60660), p(2.44200, -76.60890), p(2.44420, -76.60900), p(2.44650, -76.60860),
            p(2.44880, -76.60700), p(2.45000, -76.60400), p(2.45130, -76.60020),
        ),
    )

    /** R2 · Bello Horizonte (norte) → Terminal de Transportes. 23 puntos. */
    private val r2 = densificar(
        listOf(
            p(2.47150, -76.58620), p(2.46980, -76.58710), p(2.46800, -76.58800), p(2.46620, -76.58900),
            p(2.46430, -76.59010), p(2.46250, -76.59120), p(2.46070, -76.59230), p(2.45890, -76.59330),
            p(2.45710, -76.59420), p(2.45560, -76.59480), p(2.45430, -76.59520), p(2.45330, -76.59550),
        ),
    )

    /** R3 · La Esmeralda (suroccidente) → centro → Pomona (norte). 25 puntos. */
    private val r3 = densificar(
        listOf(
            p(2.43150, -76.61650), p(2.43280, -76.61500), p(2.43430, -76.61350), p(2.43580, -76.61200),
            p(2.43740, -76.61050), p(2.43900, -76.60900), p(2.44060, -76.60780), p(2.44190, -76.60660),
            p(2.44380, -76.60740), p(2.44570, -76.60820), p(2.44760, -76.60900), p(2.44950, -76.60980),
            p(2.45130, -76.61050),
        ),
    )

    private val r3PorLaCalle5 = densificar(
        listOf(
            p(2.43580, -76.61200), p(2.43700, -76.61320), p(2.43880, -76.61280), p(2.44050, -76.61100),
            p(2.44190, -76.60660),
        ),
    )

    private val r3PorElRioMolino = densificar(
        listOf(
            p(2.44570, -76.60820), p(2.44620, -76.61050), p(2.44800, -76.61150), p(2.44990, -76.61160),
            p(2.45130, -76.61050),
        ),
    )

    val rutas = listOf(
        RutaDemo("R1", "Centro – Campanario", "#0057B8", r1, listOf(AlternoDemo("Desvío por Cra 9", r1Desvio))),
        RutaDemo("R2", "Bello Horizonte – Terminal", "#D62828", r2),
        RutaDemo(
            "R3", "La Esmeralda – Pomona", "#2A9D8F", r3,
            listOf(AlternoDemo("Desvío por la Calle 5", r3PorLaCalle5), AlternoDemo("Desvío por el río Molino", r3PorElRioMolino)),
        ),
    )
}
