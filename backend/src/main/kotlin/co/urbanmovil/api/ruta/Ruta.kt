package co.urbanmovil.api.ruta

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * HU-02 a HU-05. El recorrido principal va en el sentido de circulación:
 * el primer punto es el inicio y el último el final (CA-02.5).
 */
@Entity
@Table(name = "ruta")
class Ruta(
    @Column(nullable = false, length = 20)
    var codigo: String,

    @Column(nullable = false, length = 120)
    var nombre: String,

    @Column(nullable = false, length = 20)
    var color: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recorrido_principal", nullable = false, columnDefinition = "jsonb")
    var recorridoPrincipal: List<Punto>,

    @Column(name = "creado_en", nullable = false, updatable = false)
    var creadoEn: Instant,

    @Column(name = "actualizado_en", nullable = false)
    var actualizadoEn: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    /** Un alterno que sale de esta lista se borra de la BD (CA-04.3). */
    @OneToMany(mappedBy = "ruta", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id")
    var alternos: MutableList<CaminoAlterno> = mutableListOf()
        protected set

    val inicio: Punto? get() = recorridoPrincipal.firstOrNull()

    val final: Punto? get() = recorridoPrincipal.lastOrNull()
}
