package co.urbanmovil.app.data

import co.urbanmovil.app.data.model.Bus
import co.urbanmovil.app.data.model.Credenciales
import co.urbanmovil.app.data.model.Lugar
import co.urbanmovil.app.data.model.NivelOcupacion
import co.urbanmovil.app.data.model.Punto
import co.urbanmovil.app.data.model.Ruta
import co.urbanmovil.app.data.model.Usuario

/**
 * Datos quemados del MVP. SOLO los repositorios falsos (`Fake*Repository`) y las pruebas
 * pueden leer este objeto; las pantallas y ViewModels pasan siempre por los repositorios.
 *
 * ## Cómo reemplazar los recorridos (DP-11)
 * Los puntos son FICTICIOS (aproximados a calles de Popayán; Parque Caldas ≈ 2.4419, -76.6066).
 * Para trazar una ruta real:
 * 1. Abrir https://geojson.io, dibujar una `LineString` siguiendo las calles EN EL SENTIDO DE CIRCULACIÓN.
 * 2. Copiar el arreglo `coordinates` del GeoJSON. OJO: GeoJSON usa el orden `[lng, lat]`.
 * 3. Convertir cada par a `p(lat, lng)` en la lista `principal` correspondiente (15–40 puntos).
 */
object FakeData {

    // ---------------------------------------------------------------- Constantes configurables

    /** Velocidad promedio usada para ETA y tiempo de trayecto. Pendiente de confirmar por el equipo. */
    const val VELOCIDAD_PROMEDIO_KMH = 18.0

    /** Caminata máxima hasta/desde la ruta (RN-08). */
    const val MAX_CAMINATA_M = 500.0

    /** Hasta esta edad la ubicación es «reciente» (RN-07). */
    const val VIGENCIA_RECIENTE_S = 60

    /** Pasada esta edad el bus se oculta (RN-07, CA-19.3). */
    const val VIGENCIA_OCULTA_MIN = 15

    /** Un reporte de ocupación más viejo que esto ya no se muestra (CA-24.2). */
    const val OCUPACION_VIGENTE_MIN = 10

    /** HU-24 es «Should» y está pendiente de aprobación (D-01): se apaga con esta bandera. */
    const val MOSTRAR_OCUPACION = true

    /** Origen y destino más cercanos que esto se consideran el mismo punto (CA-21.5). */
    const val DISTANCIA_PUNTOS_IGUALES_M = 20.0

    // ---------------------------------------------------------------- Usuario demo (HU-17)

    val USUARIO_DEMO = Usuario(correo = "pasajero@demo.com", nombre = "Pasajero demo")
    const val CLAVE_DEMO = "123456"
    val CREDENCIALES_DEMO = Credenciales(USUARIO_DEMO.correo, CLAVE_DEMO)

    // ---------------------------------------------------------------- Rutas

    private fun p(lat: Double, lng: Double) = Punto(lat, lng)

    /** R1: sale del barrio Valencia, cruza el Centro (Parque Caldas), Tulcán, Hospital y llega a Campanario. */
    val R1 = Ruta(
        id = "R1",
        nombre = "Centro – Campanario",
        colorHex = 0xFF1F4FD8,
        minutosPorVuelta = 24,
        principal = listOf(
            p(2.4335, -76.6125), // Valencia
            p(2.4345, -76.6118),
            p(2.4357, -76.6110),
            p(2.4370, -76.6102),
            p(2.4383, -76.6094),
            p(2.4395, -76.6086),
            p(2.4407, -76.6077),
            p(2.4419, -76.6066), // Parque Caldas
            p(2.4428, -76.6052),
            p(2.4436, -76.6038),
            p(2.4443, -76.6022),
            p(2.4449, -76.6005),
            p(2.4455, -76.5992), // Unicauca – Tulcán
            p(2.4472, -76.5985),
            p(2.4490, -76.5978),
            p(2.4508, -76.5970),
            p(2.4520, -76.5962), // Hospital San José
            p(2.4540, -76.5953),
            p(2.4560, -76.5945),
            p(2.4580, -76.5938),
            p(2.4605, -76.5930), // Campanario
        ),
        alternos = listOf(
            // Desvío informativo por el norte del Centro (p. ej. cierre de vía). No se usa en ETA ni búsqueda.
            listOf(
                p(2.4419, -76.6066),
                p(2.4432, -76.6075),
                p(2.4445, -76.6068),
                p(2.4452, -76.6050),
                p(2.4458, -76.6030),
                p(2.4457, -76.6010),
                p(2.4455, -76.5992),
            ),
        ),
    )

    /** R2: corredor norte, de Bello Horizonte a la Terminal de Transportes. */
    val R2 = Ruta(
        id = "R2",
        nombre = "Bello Horizonte – Terminal",
        colorHex = 0xFF5B6B88,
        minutosPorVuelta = 14,
        principal = listOf(
            p(2.4722, -76.6008), // Bello Horizonte
            p(2.4712, -76.6012),
            p(2.4702, -76.6016),
            p(2.4692, -76.6020),
            p(2.4682, -76.6023),
            p(2.4672, -76.6026),
            p(2.4662, -76.6028),
            p(2.4652, -76.6030),
            p(2.4642, -76.6032),
            p(2.4632, -76.6033),
            p(2.4622, -76.6034),
            p(2.4612, -76.6034),
            p(2.4602, -76.6034),
            p(2.4592, -76.6033),
            p(2.4581, -76.6032),
            p(2.4570, -76.6030), // Terminal
        ),
    )

    /** R3: de La Esmeralda (occidente) pasando por Valencia y Tulcán hasta Pomona (nororiente). */
    val R3 = Ruta(
        id = "R3",
        nombre = "La Esmeralda – Pomona",
        colorHex = 0xFF7A5AC8,
        minutosPorVuelta = 40,
        principal = listOf(
            p(2.4325, -76.6235), // La Esmeralda
            p(2.4327, -76.6215),
            p(2.4329, -76.6195),
            p(2.4331, -76.6175),
            p(2.4332, -76.6158),
            p(2.4333, -76.6142), // Valencia
            p(2.4345, -76.6110),
            p(2.4358, -76.6090),
            p(2.4372, -76.6070),
            p(2.4386, -76.6052),
            p(2.4400, -76.6036),
            p(2.4414, -76.6020),
            p(2.4428, -76.6006),
            p(2.4442, -76.5994),
            p(2.4455, -76.5985), // Unicauca – Tulcán
            p(2.4470, -76.5970),
            p(2.4485, -76.5952),
            p(2.4500, -76.5935),
            p(2.4515, -76.5918),
            p(2.4530, -76.5903),
            p(2.4545, -76.5892),
            p(2.4560, -76.5880), // Pomona
        ),
    )

    val rutas: List<Ruta> = listOf(R1, R2, R3)

    // ---------------------------------------------------------------- Buses

    val buses: List<Bus> = listOf(
        Bus(
            id = "UM-104", rutaId = "R1", conductor = "Carlos Muñoz", desfaseMin = 0,
            ocupacion = NivelOcupacion.CON_PUESTOS, ocupacionReportadaHaceMin = 3,
        ),
        Bus(
            // Pierde señal en los minutos 10..13 de cada vuelta → se ve «Desactualizado».
            id = "UM-221", rutaId = "R1", conductor = "Luz Dary Ordóñez", desfaseMin = 12,
            ventanaSinSenalMin = 10..13,
            // Reporte viejo (> OCUPACION_VIGENTE_MIN): no debe mostrarse (CA-24.2).
            ocupacion = NivelOcupacion.SOLO_DE_PIE, ocupacionReportadaHaceMin = 14,
        ),
        Bus(
            id = "UM-305", rutaId = "R2", conductor = "Jhon Fredy Campo", desfaseMin = 0,
            ocupacion = NivelOcupacion.LLENO, ocupacionReportadaHaceMin = 2,
        ),
        Bus(
            id = "UM-318", rutaId = "R2", conductor = "Andrea Velasco", desfaseMin = 7,
        ),
        Bus(
            // Sin señal 21 min de cada vuelta de 40 → pasa a OCULTA (CA-19.3) y R3 queda «sin buses activos».
            id = "UM-412", rutaId = "R3", conductor = "Hernán Chicangana", desfaseMin = 0,
            ventanaSinSenalMin = 5..25,
        ),
    )

    // ---------------------------------------------------------------- Lugares

    val lugares: List<Lugar> = listOf(
        Lugar("Barrio Valencia", p(2.4330, -76.6140)),
        Lugar("Universidad del Cauca – Sede Tulcán", p(2.4452, -76.5990)),
        Lugar("Parque Caldas", p(2.4419, -76.6066)),
        Lugar("Terminal de Transportes", p(2.4570, -76.6030)),
        Lugar("Centro Comercial Campanario", p(2.4605, -76.5930)),
        Lugar("Hospital San José", p(2.4520, -76.5962)),
    )

    /** «Mi ubicación» fija en el barrio Valencia (el MVP no usa GPS). */
    val MI_UBICACION = Lugar("Mi ubicación", p(2.4332, -76.6138))
}
