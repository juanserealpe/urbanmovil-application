package co.urbanmovil.api.vehiculo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** D-14 (provisional): solo placa y número interno. */
@Entity
@Table(name = "vehiculo")
class Vehiculo(
    @Column(nullable = false, length = 10)
    var placa: String,

    @Column(name = "numero_interno", nullable = false, length = 20)
    var numeroInterno: String,

    @Column(name = "creado_en", nullable = false, updatable = false)
    var creadoEn: Instant,

    @Column(name = "actualizado_en", nullable = false)
    var actualizadoEn: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var activo: Boolean = true
        protected set

    @Column(name = "motivo_inactivacion", length = 300)
    var motivoInactivacion: String? = null
        protected set

    @Column(name = "fecha_inactivacion")
    var fechaInactivacion: Instant? = null
        protected set

    /**
     * HU-30: queda constancia de por qué se retiró. Si ya estaba inactivo solo se actualiza
     * el motivo y se conserva la fecha original de inactivación.
     */
    fun desactivar(motivo: String, ahora: Instant) {
        if (activo) fechaInactivacion = ahora
        activo = false
        motivoInactivacion = motivo
        actualizadoEn = ahora
    }

    fun reactivar(ahora: Instant) {
        activo = true
        motivoInactivacion = null
        fechaInactivacion = null
        actualizadoEn = ahora
    }
}
