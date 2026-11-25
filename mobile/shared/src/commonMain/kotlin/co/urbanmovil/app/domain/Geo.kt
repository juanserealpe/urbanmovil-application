package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.Punto
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val RADIO_TIERRA_M = 6_371_000.0

private val Double.rad: Double get() = this * PI / 180.0

/** Distancia en metros entre dos puntos (fórmula de haversine). */
fun distanciaM(a: Punto, b: Punto): Double {
    val dLat = (b.lat - a.lat).rad
    val dLng = (b.lng - a.lng).rad
    val h = sin(dLat / 2).let { it * it } +
        cos(a.lat.rad) * cos(b.lat.rad) * sin(dLng / 2).let { it * it }
    return 2 * RADIO_TIERRA_M * asin(min(1.0, sqrt(h)))
}

/**
 * Resultado de proyectar un punto sobre un recorrido.
 * @property punto punto proyectado sobre el recorrido.
 * @property distanciaM distancia en línea recta desde el punto original al proyectado (caminata).
 * @property posicionM metros acumulados desde el inicio del recorrido hasta [punto].
 */
data class ProyeccionEnRecorrido(val punto: Punto, val distanciaM: Double, val posicionM: Double)

/** Metros acumulados desde el inicio hasta cada vértice. `resultado[0] == 0.0`. */
fun longitudesAcumuladas(polilinea: List<Punto>): List<Double> {
    if (polilinea.isEmpty()) return emptyList()
    val acumuladas = ArrayList<Double>(polilinea.size)
    var total = 0.0
    acumuladas += total
    for (i in 1 until polilinea.size) {
        total += distanciaM(polilinea[i - 1], polilinea[i])
        acumuladas += total
    }
    return acumuladas
}

fun longitudTotalM(polilinea: List<Punto>): Double = longitudesAcumuladas(polilinea).lastOrNull() ?: 0.0

/**
 * Punto del recorrido más cercano a [p]. En caso de empate gana el primero en el sentido de circulación.
 * La proyección se hace en un plano local (equirectangular), suficiente a escala de ciudad.
 */
fun puntoMasCercano(p: Punto, polilinea: List<Punto>): ProyeccionEnRecorrido {
    require(polilinea.isNotEmpty()) { "El recorrido no puede estar vacío" }
    if (polilinea.size == 1) {
        return ProyeccionEnRecorrido(polilinea[0], distanciaM(p, polilinea[0]), 0.0)
    }
    val acumuladas = longitudesAcumuladas(polilinea)
    var mejor: ProyeccionEnRecorrido? = null
    for (i in 0 until polilinea.size - 1) {
        val a = polilinea[i]
        val b = polilinea[i + 1]
        val t = fraccionDeProyeccion(p, a, b)
        val proyectado = interpolar(a, b, t)
        val distancia = distanciaM(p, proyectado)
        if (mejor == null || distancia < mejor.distanciaM) {
            val posicion = acumuladas[i] + t * (acumuladas[i + 1] - acumuladas[i])
            mejor = ProyeccionEnRecorrido(proyectado, distancia, posicion)
        }
    }
    return mejor!!
}

/** Metros a recorrer sobre la ruta entre dos posiciones acumuladas. Negativo si `hastaM` queda antes. */
fun distanciaSobreRecorrido(desdeM: Double, hastaM: Double): Double = hastaM - desdeM

/** Punto ubicado a [posicionM] metros del inicio, interpolando proporcional a la distancia. */
fun puntoEnPosicion(polilinea: List<Punto>, posicionM: Double): Punto {
    require(polilinea.isNotEmpty()) { "El recorrido no puede estar vacío" }
    val acumuladas = longitudesAcumuladas(polilinea)
    if (posicionM <= 0.0) return polilinea.first()
    if (posicionM >= acumuladas.last()) return polilinea.last()
    for (i in 0 until polilinea.size - 1) {
        val inicio = acumuladas[i]
        val fin = acumuladas[i + 1]
        if (posicionM < fin) {
            val largo = fin - inicio
            val t = if (largo == 0.0) 0.0 else (posicionM - inicio) / largo
            return interpolar(polilinea[i], polilinea[i + 1], t)
        }
    }
    return polilinea.last()
}

/**
 * Sub-recorrido entre dos posiciones acumuladas (HU-23: tramo en bus de abordaje a descenso).
 * Incluye los puntos interpolados de inicio y fin, y los vértices intermedios en orden.
 */
fun tramoEntre(polilinea: List<Punto>, desdeM: Double, hastaM: Double): List<Punto> {
    require(polilinea.isNotEmpty()) { "El recorrido no puede estar vacío" }
    require(desdeM <= hastaM) { "El tramo debe ir en el sentido de circulación" }
    val acumuladas = longitudesAcumuladas(polilinea)
    val intermedios = polilinea.indices.filter { acumuladas[it] > desdeM && acumuladas[it] < hastaM }.map { polilinea[it] }
    return listOf(puntoEnPosicion(polilinea, desdeM)) + intermedios + puntoEnPosicion(polilinea, hastaM)
}

/** Rectángulo geográfico que encierra un conjunto de puntos (para encuadrar el mapa). */
data class Limites(val sur: Double, val oeste: Double, val norte: Double, val este: Double)

/** Límites de [puntos], o `null` si la lista está vacía. */
fun limites(puntos: List<Punto>): Limites? {
    if (puntos.isEmpty()) return null
    return Limites(
        sur = puntos.minOf { it.lat },
        oeste = puntos.minOf { it.lng },
        norte = puntos.maxOf { it.lat },
        este = puntos.maxOf { it.lng },
    )
}

private fun interpolar(a: Punto, b: Punto, t: Double) =
    Punto(a.lat + (b.lat - a.lat) * t, a.lng + (b.lng - a.lng) * t)

/** Fracción [0, 1] del segmento a→b donde cae la proyección ortogonal de p. */
private fun fraccionDeProyeccion(p: Punto, a: Punto, b: Punto): Double {
    val cosLat = cos(((a.lat + b.lat) / 2).rad)
    val abx = (b.lng - a.lng) * cosLat
    val aby = b.lat - a.lat
    val apx = (p.lng - a.lng) * cosLat
    val apy = p.lat - a.lat
    val largo2 = abx * abx + aby * aby
    if (largo2 == 0.0) return 0.0
    return ((apx * abx + apy * aby) / largo2).coerceIn(0.0, 1.0)
}
