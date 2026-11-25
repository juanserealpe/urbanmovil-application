package co.urbanmovil.app.domain

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.EstadoBus
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.data.model.Vigencia

/** Datos de prueba controlados: NO dependen de FakeData para que las pruebas sean deterministas. */
object Fixtures {
    const val LAT = 2.44
    const val VELOCIDAD_KMH = 18.0 // 300 m/min

    /** Línea recta oeste → este a lo largo de LAT, 11 puntos, ≈ 2.2 km. */
    val recta: List<Punto> = (0..10).map { i -> Punto(LAT, -76.62 + i * 0.002) }

    fun ruta(
        id: String,
        principal: List<Punto>,
        alternos: List<List<Punto>> = emptyList(),
        minutosPorVuelta: Int = 20,
    ) = Ruta(
        id = id,
        nombre = "Ruta $id",
        colorHex = 0xFF000000,
        principal = principal,
        alternos = alternos,
        minutosPorVuelta = minutosPorVuelta,
    )

    val rutaRecta = ruta("RT", recta)

    fun estado(
        rutaId: String,
        posicionM: Double,
        vigencia: Vigencia = Vigencia.RECIENTE,
        busId: String = "B-$rutaId-$posicionM",
    ) = EstadoBus(
        bus = Bus(id = busId, rutaId = rutaId, conductor = "Prueba", desfaseMin = 0),
        posicion = Punto(LAT, -76.62),
        posicionEnRecorridoM = posicionM,
        horaUltimaUbicacion = 0L,
        vigencia = vigencia,
    )
}
