package co.urbanmovil.api.mapa

/**
 * Estilo MapLibre (spec v8) de la ciudad ficticia, con el GeoJSON EMBEBIDO: web y app cargan
 * exactamente este JSON, así el mapa se ve igual en las dos y no depende de un servidor de teselas.
 *
 * Lo único externo son las fuentes de las etiquetas (OpenFreeMap). Si no cargan, el mapa se ve
 * igual pero sin nombres de calles.
 */
object EstiloMapa {

    private const val FUENTE = "ciudad"
    private val FUENTE_NORMAL = listOf("Noto Sans Regular")
    private val FUENTE_NEGRITA = listOf("Noto Sans Bold")

    private object Color {
        const val SUELO = "#F2EFE9"
        const val MANZANA = "#E3DDD2"
        const val PARQUE = "#C8E6B9"
        const val RIO = "#A9D3F0"
        const val CALLE = "#FFFFFF"
        const val BORDE_CALLE = "#D6CFC2"
        const val AVENIDA = "#FFF1B8"
        const val BORDE_AVENIDA = "#E2C66E"
        const val TEXTO = "#5F5A52"
        const val TEXTO_PARQUE = "#3F6B2E"
        const val TEXTO_RIO = "#2D6E99"
    }

    private fun filtroTipo(tipo: String) = listOf("==", listOf("get", "tipo"), tipo)

    /** Ancho que crece con el zoom, como en un mapa real (en píxeles a cada zoom). */
    private fun anchoPorZoom(vararg paradas: Pair<Number, Number>): List<Any> =
        listOf<Any>("interpolate", listOf("exponential", 1.6), listOf("zoom")) + paradas.flatMap { listOf(it.first, it.second) }

    private fun relleno(id: String, tipo: String, color: String) = mapOf(
        "id" to id, "type" to "fill", "source" to FUENTE, "filter" to filtroTipo(tipo),
        "paint" to mapOf("fill-color" to color),
    )

    private fun linea(id: String, tipo: String, color: String, ancho: List<Any>) = mapOf(
        "id" to id, "type" to "line", "source" to FUENTE, "filter" to filtroTipo(tipo),
        "layout" to mapOf("line-join" to "round", "line-cap" to "round"),
        "paint" to mapOf("line-color" to color, "line-width" to ancho),
    )

    private fun nombres(id: String, tipo: String, fuente: List<String>, tamano: Number, color: String, minzoom: Number, enLinea: Boolean) = mapOf(
        "id" to id, "type" to "symbol", "source" to FUENTE, "filter" to filtroTipo(tipo), "minzoom" to minzoom,
        "layout" to buildMap {
            put("text-field", listOf("get", "nombre"))
            put("text-font", fuente)
            put("text-size", tamano)
            if (enLinea) {
                put("symbol-placement", "line")
                put("symbol-spacing", 350)
            }
        },
        "paint" to mapOf("text-color" to color, "text-halo-color" to "#FFFFFF", "text-halo-width" to 1.5),
    )

    fun estilo(): Map<String, Any> = mapOf(
        "version" to 8,
        "name" to "UrbanMovil · ciudad ficticia",
        "glyphs" to "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
        "sources" to mapOf(FUENTE to mapOf("type" to "geojson", "data" to CiudadFicticia.geoJson())),
        "layers" to listOf(
            mapOf("id" to "suelo", "type" to "background", "paint" to mapOf("background-color" to Color.SUELO)),
            relleno("rio", "rio", Color.RIO),
            relleno("manzanas", "manzana", Color.MANZANA),
            relleno("parques", "parque", Color.PARQUE),
            // Borde debajo y relleno encima: el efecto «calle con andén» de los mapas de verdad.
            linea("calles-borde", "calle", Color.BORDE_CALLE, anchoPorZoom(13 to 2, 16 to 12, 19 to 60)),
            linea("calles", "calle", Color.CALLE, anchoPorZoom(13 to 1, 16 to 9, 19 to 52)),
            linea("avenidas-borde", "avenida", Color.BORDE_AVENIDA, anchoPorZoom(13 to 4, 16 to 18, 19 to 90)),
            linea("avenidas", "avenida", Color.AVENIDA, anchoPorZoom(13 to 3, 16 to 15, 19 to 80)),
            nombres("nombres-calles", "calle", FUENTE_NORMAL, 11, Color.TEXTO, 15.5, enLinea = true),
            nombres("nombres-avenidas", "avenida", FUENTE_NEGRITA, 12, Color.TEXTO, 13.5, enLinea = true),
            nombres("nombres-parques", "nombre-parque", FUENTE_NEGRITA, 12, Color.TEXTO_PARQUE, 14, enLinea = false),
            nombres("nombre-rio", "rio", FUENTE_NORMAL, 12, Color.TEXTO_RIO, 13, enLinea = false),
        ),
    )
}
