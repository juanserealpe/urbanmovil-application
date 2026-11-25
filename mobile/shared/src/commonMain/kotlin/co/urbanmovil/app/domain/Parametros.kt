package co.urbanmovil.app.domain

import co.urbanmovil.app.data.FakeData
import kotlin.math.roundToInt

/**
 * Caminata máxima (RN-08) redondeada para textos («máx. 500 m a pie», «a más de 500 m»).
 * La UI la toma de aquí para no leer [FakeData] directamente.
 */
val MAX_CAMINATA_TEXTO_M: Int get() = FakeData.MAX_CAMINATA_M.roundToInt()
