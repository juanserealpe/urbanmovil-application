package co.urbanmovil.api.vehiculo

import org.springframework.data.jpa.repository.JpaRepository

interface VehiculoRepository : JpaRepository<Vehiculo, Long> {

    fun existsByPlacaIgnoreCase(placa: String): Boolean

    fun existsByPlacaIgnoreCaseAndIdNot(placa: String, id: Long): Boolean

    fun existsByNumeroInternoIgnoreCase(numeroInterno: String): Boolean

    fun existsByNumeroInternoIgnoreCaseAndIdNot(numeroInterno: String, id: Long): Boolean
}
