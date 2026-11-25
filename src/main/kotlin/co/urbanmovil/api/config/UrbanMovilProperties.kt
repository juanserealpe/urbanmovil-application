package co.urbanmovil.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "urbanmovil")
data class UrbanMovilProperties(
    val zonaHoraria: String = "America/Bogota",
    val adminInicial: AdminInicial = AdminInicial(),
    val cors: Cors = Cors(),
    val demo: Demo = Demo(),
    val simulacion: Simulacion = Simulacion(),
) {
    data class AdminInicial(
        val nombre: String = "Administrador",
        val correo: String = "admin@urbanmovil.co",
        val contrasena: String = "Admin12345",
    )

    data class Cors(
        val origenes: List<String> = listOf("http://localhost:5173"),
    )

    data class Demo(
        val habilitado: Boolean = false,
    )

    /** Monitoreo simulado (HU-31): velocidad constante del bus sobre el recorrido principal. */
    data class Simulacion(
        val velocidadKmh: Double = 18.0,
    )
}
