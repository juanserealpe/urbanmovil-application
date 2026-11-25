package co.urbanmovil.api.conductor

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

enum class MotivoNoDisponible { INCAPACIDAD, VACACIONES, PERMISO, OTRO }

@Entity
@Table(name = "conductor")
class Conductor(
    @Column(nullable = false, length = 120)
    var nombre: String,

    @Column(nullable = false, length = 20)
    var identificacion: String,

    @Column(nullable = false, length = 120)
    var correo: String,

    @Column(nullable = false, length = 20)
    var telefono: String,

    @Column(name = "hash_contrasena", nullable = false, length = 100)
    var hashContrasena: String,

    @Column(name = "creado_en", nullable = false, updatable = false)
    var creadoEn: Instant,

    @Column(name = "actualizado_en", nullable = false)
    var actualizadoEn: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var disponible: Boolean = true
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_no_disponible", length = 20)
    var motivoNoDisponible: MotivoNoDisponible? = null
        protected set

    @Column(name = "detalle_motivo", length = 300)
    var detalleMotivo: String? = null
        protected set

    /** RN-06/RN-13: un conductor no disponible siempre tiene motivo. */
    fun marcarNoDisponible(motivo: MotivoNoDisponible, detalle: String?, ahora: Instant) {
        disponible = false
        motivoNoDisponible = motivo
        detalleMotivo = detalle
        actualizadoEn = ahora
    }

    fun marcarDisponible(ahora: Instant) {
        disponible = true
        motivoNoDisponible = null
        detalleMotivo = null
        actualizadoEn = ahora
    }
}
