package co.urbanmovil.api.common

/** Utilidades de normalización de texto. */
object Texto {

    /** `abc-123`, ` abc 123 ` → `ABC123`. */
    fun normalizarPlaca(placa: String): String =
        placa.replace(Regex("[\\s-]"), "").uppercase()

    /** Recorta y pasa a mayúsculas (`um-104` → `UM-104`). */
    fun mayusculas(valor: String): String = valor.trim().uppercase()

    /** Correos: se guardan recortados y en minúsculas. */
    fun normalizarCorreo(correo: String): String = correo.trim().lowercase()

    /** `null` si el texto viene vacío o solo con espacios. */
    fun vacioANulo(valor: String?): String? = valor?.trim()?.takeIf { it.isNotEmpty() }

    /** Búsqueda insensible a mayúsculas. */
    fun contiene(valor: String?, busqueda: String): Boolean =
        valor?.contains(busqueda, ignoreCase = true) ?: false
}
