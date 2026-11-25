package co.urbanmovil.api.reasignacion

import co.urbanmovil.api.administrador.Administrador
import co.urbanmovil.api.conductor.Conductor
import co.urbanmovil.api.ruta.Ruta
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

enum class TipoReasignacion { ASIGNADA, QUITADA }

/** HU-25: constancia de una reasignación por eventualidad (RN-23). */
@Entity
@Table(name = "reasignacion")
class Reasignacion(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conductor_id", nullable = false)
    var conductor: Conductor,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrador_id", nullable = false)
    var administrador: Administrador,

    @Column(name = "fecha_hora", nullable = false)
    var fechaHora: Instant,

    @Column(nullable = false, length = 300)
    var motivo: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "reasignacion", cascade = [CascadeType.ALL], orphanRemoval = true)
    var rutas: MutableList<ReasignacionRuta> = mutableListOf()
        protected set

    fun registrar(ruta: Ruta, tipo: TipoReasignacion) {
        rutas.add(ReasignacionRuta(this, ruta, tipo))
    }

    fun codigos(tipo: TipoReasignacion): List<String> =
        rutas.filter { it.tipo == tipo }.sortedBy { it.id ?: Long.MAX_VALUE }.map { it.ruta.codigo }
}

@Entity
@Table(name = "reasignacion_ruta")
class ReasignacionRuta(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reasignacion_id", nullable = false)
    var reasignacion: Reasignacion,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    var ruta: Ruta,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var tipo: TipoReasignacion,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
