package co.urbanmovil.api.ruta

import co.urbanmovil.api.common.ValidacionException

/**
 * Reglas de los trazados (§4 de la Fase 2). Lanza [ValidacionException] con el campo
 * afectado (`recorridoPrincipal`, `alternos[1].puntos`, ...) y devuelve los puntos ya limpios.
 */
object TrazadoValidador {

    const val MAX_PUNTOS = 2_000
    const val MAX_ALTERNOS = 10

    private const val MSG_FALTA_PRINCIPAL = "Falta trazar el recorrido principal"
    private const val MSG_INICIO_FINAL = "La ruta debe tener punto de inicio y punto final"
    private const val MSG_ALTERNO_CORTO = "Cada camino alterno debe tener al menos dos puntos"
    private const val MSG_COORDENADAS = "Coordenadas inválidas"
    private const val MSG_MAX_PUNTOS = "El trazado supera el máximo de puntos permitido"
    private const val MSG_MAX_ALTERNOS = "Una ruta admite máximo $MAX_ALTERNOS caminos alternos"

    /** CA-02.2, CA-02.4, CA-04.2. */
    fun recorridoPrincipal(puntos: List<PuntoRequest?>?): List<Punto> {
        val campo = "recorridoPrincipal"
        if (puntos.isNullOrEmpty()) throw ValidacionException(campo, MSG_FALTA_PRINCIPAL)
        if (puntos.size < 2) throw ValidacionException(campo, MSG_INICIO_FINAL)
        return comunes(campo, puntos)
    }

    fun alternos(alternos: List<CaminoAlternoRequest>?): List<CaminoAlternoRequest> {
        val lista = alternos.orEmpty()
        if (lista.size > MAX_ALTERNOS) throw ValidacionException("alternos", MSG_MAX_ALTERNOS)
        return lista
    }

    fun puntosAlterno(indice: Int, puntos: List<PuntoRequest?>?): List<Punto> {
        val campo = "alternos[$indice].puntos"
        if (puntos == null || puntos.size < 2) throw ValidacionException(campo, MSG_ALTERNO_CORTO)
        return comunes(campo, puntos)
    }

    private fun comunes(campo: String, puntos: List<PuntoRequest?>): List<Punto> {
        if (puntos.size > MAX_PUNTOS) throw ValidacionException(campo, MSG_MAX_PUNTOS)
        return puntos.map { punto ->
            val lat = punto?.lat
            val lng = punto?.lng
            if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                throw ValidacionException(campo, MSG_COORDENADAS)
            }
            Punto(lat, lng)
        }
    }
}
