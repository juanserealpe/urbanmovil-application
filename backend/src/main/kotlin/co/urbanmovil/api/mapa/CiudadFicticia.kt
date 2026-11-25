package co.urbanmovil.api.mapa

import co.urbanmovil.api.ruta.Punto
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sign

/**
 * Ciudad FICTICIA sobre la que corre el demo: una cuadrícula al estilo colombiano, con
 * carreras (norte–sur, índice `carrera` = x) y calles (oriente–occidente, índice `calle` = y).
 *
 * - Cuadras de 0,001° (~111 m). Esquinas de 0 a [ULTIMA]; la (10, 10) es el Parque Central,
 *   en las coordenadas de Popayán para no mover la cámara inicial de web y app.
 * - Cada 5 carreras/calles hay una avenida con nombre; además la Diagonal del Sol, que pasa
 *   exactamente por las esquinas (i, i).
 *
 * Es la ÚNICA fuente de verdad de las calles: con ella se dibuja el mapa ([geoJson]) y se arman
 * los recorridos ([trazado]), así que un recorrido de demo no puede pasar por donde no hay calle.
 */
object CiudadFicticia {

    const val ULTIMA = 20
    private const val PASO = 0.001
    private const val LAT_BASE = 2.434
    private const val LNG_BASE = -76.616
    private const val CADA_AVENIDA = 5

    /** Tolerancia para decidir si un punto está sobre el eje de una calle (~1 cm). */
    private const val TOLERANCIA = 1e-7

    /** Separación entre el eje de la calle y el borde de la manzana (~13 m): el «ancho» visible de la calle. */
    private const val RETIRO_MANZANA = 0.00012

    private val avenidasCarrera = mapOf(
        0 to "Av. Occidental", 5 to "Av. Libertadores", 10 to "Av. Central", 15 to "Av. Las Palmas", 20 to "Av. Oriental",
    )
    private val avenidasCalle = mapOf(
        0 to "Av. del Río", 5 to "Av. Sur", 10 to "Av. Bolívar", 15 to "Av. Norte", 20 to "Av. Circunvalar",
    )
    private const val DIAGONAL = "Diagonal del Sol"

    /** Manzanas que son parque, por su esquina suroccidental (carrera, calle). */
    private val parques = mapOf(
        "Parque Central" to listOf(9 to 9, 9 to 10, 10 to 9, 10 to 10),
        "Parque del Río" to listOf(2 to 0, 3 to 0, 4 to 0),
        "Parque Norte" to listOf(16 to 17, 17 to 17, 16 to 18, 17 to 18),
    )

    private fun redondear(valor: Double) = round(valor * 1_000_000) / 1_000_000

    /** Coordenada de la esquina de la carrera [carrera] con la calle [calle]. */
    fun esquina(carrera: Int, calle: Int): Punto {
        require(carrera in 0..ULTIMA && calle in 0..ULTIMA) { "La esquina ($carrera, $calle) está fuera de la ciudad" }
        return Punto(redondear(LAT_BASE + calle * PASO), redondear(LNG_BASE + carrera * PASO))
    }

    private fun indice(valor: Double, base: Double): Double = (valor - base) / PASO
    private fun esEntero(x: Double) = abs(x - round(x)) < TOLERANCIA / PASO
    private fun enRango(x: Double) = x >= -TOLERANCIA / PASO && x <= ULTIMA + TOLERANCIA / PASO

    /**
     * Recorrido que une las [esquinas] dadas yendo SIEMPRE por la calle: entre cada par consecutivo
     * debe haber una carrera, una calle o la diagonal en común. Devuelve todas las esquinas intermedias
     * (un punto por cuadra), en orden. Lanza si algún tramo atravesaría manzanas.
     */
    fun trazado(vararg esquinas: Punto): List<Punto> {
        require(esquinas.size >= 2) { "Un trazado necesita al menos dos esquinas" }
        val indices = esquinas.map { p ->
            val x = indice(p.lng, LNG_BASE)
            val y = indice(p.lat, LAT_BASE)
            require(esEntero(x) && esEntero(y) && enRango(x) && enRango(y)) { "$p no es una esquina de la ciudad" }
            round(x).toInt() to round(y).toInt()
        }
        val puntos = mutableListOf(esquina(indices[0].first, indices[0].second))
        indices.zipWithNext().forEach { (a, b) ->
            val dx = b.first - a.first
            val dy = b.second - a.second
            val porCarreraOCalle = dx == 0 || dy == 0
            val porLaDiagonal = a.first == a.second && b.first == b.second
            require(porCarreraOCalle || porLaDiagonal) { "El tramo $a → $b no va por ninguna calle" }
            val pasos = max(abs(dx), abs(dy))
            for (i in 1..pasos) puntos += esquina(a.first + i * dx.sign, a.second + i * dy.sign)
        }
        return puntos
    }

    /** ¿El punto está sobre el eje de alguna carrera, calle o de la diagonal, dentro de la ciudad? */
    fun estaSobreCalle(p: Punto): Boolean {
        val x = indice(p.lng, LNG_BASE)
        val y = indice(p.lat, LAT_BASE)
        if (!enRango(x) || !enRango(y)) return false
        return esEntero(x) || esEntero(y) || abs(x - y) < TOLERANCIA / PASO
    }

    // ---------- Lugares (búsqueda de viaje en la app) ----------

    data class Lugar(val nombre: String, val punto: Punto)

    /** Todos sobre una calle y cerca de alguna ruta de demo. */
    val lugares: List<Lugar> = listOf(
        Lugar("Parque Central", esquina(10, 11)),
        Lugar("Terminal de Transportes", esquina(17, 11)),
        Lugar("Hospital Central", esquina(12, 16)),
        Lugar("Universidad Regional", esquina(18, 5)),
        Lugar("Centro Comercial Libertadores", esquina(8, 18)),
        Lugar("Estadio Municipal", esquina(6, 4)),
        Lugar("Barrio La Ribera", esquina(4, 2)),
        Lugar("Plaza de Mercado", esquina(19, 10)),
    )

    /** «Mi ubicación» del pasajero: el MVP no tiene GPS, es un punto fijo. */
    val ubicacionPasajero = Lugar("Mi ubicación", esquina(3, 6))

    // ---------- Lo que se dibuja ----------

    data class Geometria(val type: String, val coordinates: Any)
    data class Feature(val properties: Map<String, Any>, val geometry: Geometria) {
        val type = "Feature"
    }
    data class FeatureCollection(val features: List<Feature>) {
        val type = "FeatureCollection"
    }

    /** GeoJSON usa [lng, lat]. */
    private fun coord(p: Punto) = listOf(p.lng, p.lat)
    private fun coord(lat: Double, lng: Double) = listOf(redondear(lng), redondear(lat))

    private fun linea(tipo: String, nombre: String, puntos: List<Punto>) =
        Feature(mapOf("tipo" to tipo, "nombre" to nombre), Geometria("LineString", puntos.map(::coord)))

    private fun poligono(tipo: String, anillo: List<List<Double>>, nombre: String? = null) = Feature(
        buildMap { put("tipo", tipo); if (nombre != null) put("nombre", nombre) },
        Geometria("Polygon", listOf(anillo + listOf(anillo.first()))),
    )

    fun geoJson(): FeatureCollection {
        val features = mutableListOf<Feature>()
        features += rio()
        features += manzanas()
        features += parques.map { (nombre, cuadras) ->
            // Punto en el centro del parque para su etiqueta.
            val lat = cuadras.map { LAT_BASE + (it.second + 0.5) * PASO }.average()
            val lng = cuadras.map { LNG_BASE + (it.first + 0.5) * PASO }.average()
            Feature(mapOf("tipo" to "nombre-parque", "nombre" to nombre), Geometria("Point", coord(lat, lng)))
        }
        for (i in 0..ULTIMA) {
            val carrera = (0..ULTIMA).map { esquina(i, it) }
            val calle = (0..ULTIMA).map { esquina(it, i) }
            features += avenidasCarrera[i]?.let { linea("avenida", it, carrera) } ?: linea("calle", "Carrera $i", carrera)
            features += avenidasCalle[i]?.let { linea("avenida", it, calle) } ?: linea("calle", "Calle $i", calle)
        }
        features += linea("avenida", DIAGONAL, (0..ULTIMA).map { esquina(it, it) })
        return FeatureCollection(features)
    }

    private fun tipoDeManzana(carrera: Int, calle: Int): Pair<String, String?> {
        val parque = parques.entries.firstOrNull { (carrera to calle) in it.value }
        return if (parque != null) "parque" to parque.key else "manzana" to null
    }

    /** Una manzana por cuadra, retirada del eje de las calles. Las que cruza la diagonal se parten en dos triángulos. */
    private fun manzanas(): List<Feature> = buildList {
        val r = RETIRO_MANZANA
        for (x in 0 until ULTIMA) for (y in 0 until ULTIMA) {
            val oeste = LNG_BASE + x * PASO
            val este = oeste + PASO
            val sur = LAT_BASE + y * PASO
            val norte = sur + PASO
            val (tipo, nombre) = tipoDeManzana(x, y)
            if (x != y) {
                add(poligono(tipo, listOf(coord(sur + r, oeste + r), coord(sur + r, este - r), coord(norte - r, este - r), coord(norte - r, oeste + r)), nombre))
            } else {
                // La diagonal va de la esquina suroccidental a la nororiental: dos triángulos, cada uno retirado de sus tres calles.
                val d = r * 2.414 // retiro sobre la diagonal en el vértice agudo (1 + √2)
                add(poligono(tipo, listOf(coord(sur + r, oeste + d), coord(sur + r, este - r), coord(norte - d, este - r)), nombre))
                add(poligono(tipo, listOf(coord(sur + d, oeste + r), coord(norte - r, este - d), coord(norte - r, oeste + r)), nombre))
            }
        }
    }

    /** Río al sur de la Av. del Río, fuera de la cuadrícula: no lo cruza ninguna calle. */
    private fun rio(): Feature {
        val orilla = (0..40).map { i -> LNG_BASE - 0.002 + i * (ULTIMA * PASO + 0.004) / 40 }
        val ondulacion = { i: Int -> if (i % 2 == 0) 0.00006 else -0.00006 }
        val norte = orilla.mapIndexed { i, lng -> coord(LAT_BASE - 0.0004 + ondulacion(i), lng) }
        val sur = orilla.mapIndexed { i, lng -> coord(LAT_BASE - 0.0011 + ondulacion(i + 1), lng) }.reversed()
        return poligono("rio", norte + sur, "Río Claro")
    }
}
