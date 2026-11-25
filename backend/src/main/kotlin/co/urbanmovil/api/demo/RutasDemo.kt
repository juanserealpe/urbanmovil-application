package co.urbanmovil.api.demo

import co.urbanmovil.api.mapa.CiudadFicticia.esquina
import co.urbanmovil.api.mapa.CiudadFicticia.trazado
import co.urbanmovil.api.ruta.Punto

/**
 * Rutas de demostración sobre la ciudad ficticia ([co.urbanmovil.api.mapa.CiudadFicticia]).
 *
 * Se arman SOLO con esquinas `esquina(carrera, calle)` unidas por [trazado], que lanza si un tramo
 * no va por una carrera, una calle o la Diagonal del Sol: es imposible que un bus atraviese manzanas.
 *
 * Cada recorrido principal es un CIRCUITO (termina donde empieza): el simulador da vueltas, y así
 * al reiniciar la vuelta el bus no «salta» de un extremo de la ciudad al otro.
 * Cada alterno sale de una esquina del recorrido principal y vuelve a otra.
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

    /** R1 · Av. Libertadores al norte, Calle 18, Carrera 12, Calle 14, Carrera 17 (Terminal) y regreso por la Calle 8. */
    private val r1 = trazado(
        esquina(5, 8), esquina(5, 18), esquina(12, 18), esquina(12, 14), esquina(17, 14), esquina(17, 8), esquina(5, 8),
    )
    private val r1PorLaCalle12 = trazado(esquina(5, 12), esquina(12, 12), esquina(12, 14))

    /** R2 · Diagonal del Sol hasta el nororiente, baja por la Carrera 18 (Universidad) y vuelve por la Calle 2 (La Ribera). */
    private val r2 = trazado(esquina(2, 2), esquina(18, 18), esquina(18, 2), esquina(2, 2))
    private val r2PorLaAvCentral = trazado(esquina(10, 10), esquina(10, 2))

    /** R3 · Circuito occidente–oriente: Calle 4 (Estadio), Carrera 19 (Plaza de Mercado) y regreso por la Calle 12 y la Calle 9. */
    private val r3 = trazado(
        esquina(1, 4), esquina(14, 4), esquina(14, 7), esquina(19, 7), esquina(19, 12),
        esquina(8, 12), esquina(8, 9), esquina(1, 9), esquina(1, 4),
    )
    private val r3PorLaCalle6 = trazado(esquina(1, 6), esquina(14, 6))

    val rutas = listOf(
        RutaDemo("R1", "Libertadores – Terminal", "#0057B8", r1, listOf(AlternoDemo("Desvío por la Calle 12", r1PorLaCalle12))),
        RutaDemo("R2", "Diagonal del Sol – Universidad", "#D62828", r2, listOf(AlternoDemo("Desvío por la Av. Central", r2PorLaAvCentral))),
        RutaDemo("R3", "Estadio – Plaza de Mercado", "#2A9D8F", r3, listOf(AlternoDemo("Desvío por la Calle 6", r3PorLaCalle6))),
    )
}
