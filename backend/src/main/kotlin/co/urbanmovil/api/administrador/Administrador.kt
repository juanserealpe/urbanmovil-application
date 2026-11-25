package co.urbanmovil.api.administrador

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "administrador")
class Administrador(
    @Column(nullable = false, length = 120)
    var nombre: String,

    @Column(nullable = false, length = 120)
    var correo: String,

    @Column(name = "hash_contrasena", nullable = false, length = 100)
    var hashContrasena: String,

    @Column(name = "creado_en", nullable = false, updatable = false)
    var creadoEn: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
