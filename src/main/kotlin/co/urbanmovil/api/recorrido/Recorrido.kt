package co.urbanmovil.api.recorrido

import co.urbanmovil.api.conductor.Conductor
import co.urbanmovil.api.ruta.Ruta
import co.urbanmovil.api.vehiculo.Vehiculo
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
import jakarta.persistence.Table
import java.time.Instant

enum class EstadoRecorrido { ACTIVO, FINALIZADO }

/** En Fase 1 solo se consulta (¿tiene recorrido activo?). Se crea en la Fase 3. */
@Entity
@Table(name = "recorrido")
class Recorrido(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conductor_id", nullable = false)
    var conductor: Conductor,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    var ruta: Ruta,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    var vehiculo: Vehiculo,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var estado: EstadoRecorrido,

    @Column(nullable = false)
    var inicio: Instant,

    var fin: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
