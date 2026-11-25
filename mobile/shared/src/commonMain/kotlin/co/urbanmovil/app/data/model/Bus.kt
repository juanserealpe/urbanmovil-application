package co.urbanmovil.app.data.model

enum class NivelOcupacion { CON_PUESTOS, SOLO_DE_PIE, LLENO }

data class Bus(
    /** Placa o código interno, p. ej. "UM-104". */
    val id: String,
    val rutaId: String,
    /** Solo informativo. */
    val conductor: String,
    /** Minutos de adelanto sobre la vuelta; separa los buses de una misma ruta. */
    val desfaseMin: Int,
    /** Minutos de cada vuelta en los que el bus "pierde señal" (queda congelado). */
    val ventanaSinSenalMin: IntRange? = null,
    val ocupacion: NivelOcupacion? = null,
    val ocupacionReportadaHaceMin: Int? = null,
)
