package co.urbanmovil.api.programacion

import co.urbanmovil.api.conductor.Conductor
import co.urbanmovil.api.ruta.Ruta
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate

/** HU-09: rutas que hará un conductor en una fecha, y en qué orden (V-5: se guarda por fecha). */
@Entity
@Table(name = "programacion")
class Programacion(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conductor_id", nullable = false)
    var conductor: Conductor,

    @Column(nullable = false)
    var fecha: LocalDate,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    /** Una ruta que sale de esta lista se borra de `programacion_ruta`. */
    @OneToMany(mappedBy = "programacion", cascade = [CascadeType.ALL], orphanRemoval = true)
    var rutas: MutableList<ProgramacionRuta> = mutableListOf()
        protected set

    fun entrada(rutaId: Long): ProgramacionRuta? = rutas.firstOrNull { it.ruta.id == rutaId }

    fun tiene(rutaId: Long): Boolean = entrada(rutaId) != null

    fun ordenadas(): List<ProgramacionRuta> = rutas.sortedBy { it.orden }

    fun agregarAlFinal(ruta: Ruta) {
        rutas.add(ProgramacionRuta(this, ruta, orden = (rutas.maxOfOrNull { it.orden } ?: 0) + 1))
    }

    fun quitar(rutaId: Long) {
        rutas.removeIf { it.ruta.id == rutaId }
        renumerar()
    }

    /** Deja el orden compacto (1, 2, 3...) respetando el orden relativo. */
    fun renumerar() {
        ordenadas().forEachIndexed { i, entrada -> entrada.orden = i + 1 }
    }
}

@Entity
@Table(name = "programacion_ruta")
class ProgramacionRuta(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programacion_id", nullable = false)
    var programacion: Programacion,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    var ruta: Ruta,

    @Column(nullable = false)
    var orden: Int,

    /** D-11: se quitó mientras el conductor la recorría; sale de la programación al finalizar. */
    @Column(name = "retiro_pendiente", nullable = false)
    var retiroPendiente: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
