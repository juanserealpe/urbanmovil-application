package co.urbanmovil.api.demo

import co.urbanmovil.api.administrador.AdministradorRepository
import co.urbanmovil.api.common.Texto
import co.urbanmovil.api.conductor.Conductor
import co.urbanmovil.api.conductor.ConductorRepository
import co.urbanmovil.api.conductor.MotivoNoDisponible
import co.urbanmovil.api.config.UrbanMovilProperties
import co.urbanmovil.api.programacion.ProgramacionRepository
import co.urbanmovil.api.programacion.ProgramacionService
import co.urbanmovil.api.reasignacion.ReasignacionRepository
import co.urbanmovil.api.reasignacion.ReasignacionRequest
import co.urbanmovil.api.reasignacion.ReasignacionService
import co.urbanmovil.api.recorrido.RecorridoRepository
import co.urbanmovil.api.ruta.CaminoAlterno
import co.urbanmovil.api.ruta.Ruta
import co.urbanmovil.api.ruta.RutaRepository
import co.urbanmovil.api.vehiculo.Vehiculo
import co.urbanmovil.api.vehiculo.VehiculoRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

/**
 * Datos semilla del perfil `demo` (urbanmovil.demo.habilitado=true).
 * Cada grupo se siembra solo si su tabla está vacía. Programación, recorridos y reasignación
 * pasan por los mismos servicios de la API, así que cumplen las mismas reglas.
 * La programación y los recorridos se siembran para «hoy» (según el Clock).
 * Todos los nombres y documentos son ficticios.
 */
@Component
@Order(10)
@ConditionalOnProperty(prefix = "urbanmovil.demo", name = ["habilitado"], havingValue = "true")
class DemoDataSeeder(
    private val conductorRepository: ConductorRepository,
    private val vehiculoRepository: VehiculoRepository,
    private val rutaRepository: RutaRepository,
    private val programacionRepository: ProgramacionRepository,
    private val recorridoRepository: RecorridoRepository,
    private val reasignacionRepository: ReasignacionRepository,
    private val administradorRepository: AdministradorRepository,
    private val programacionService: ProgramacionService,
    private val demoRecorridoService: DemoRecorridoService,
    private val reasignacionService: ReasignacionService,
    private val propiedades: UrbanMovilProperties,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        sembrarConductores()
        sembrarVehiculos()
        sembrarRutas()
        sembrarProgramacion()
        sembrarRecorridos()
        sembrarReasignacion()
    }

    private fun sembrarConductores() {
        if (conductorRepository.count() > 0) return
        val ahora = clock.instant()
        val hash = passwordEncoder.encode(CONTRASENA_CONDUCTORES)!!

        fun conductor(nombre: String, identificacion: String, correo: String, telefono: String) = Conductor(
            nombre = nombre,
            identificacion = identificacion,
            correo = correo,
            telefono = telefono,
            hashContrasena = hash,
            creadoEn = ahora,
            actualizadoEn = ahora,
        )

        val conductores = listOf(
            conductor("Andrés Felipe Muñoz Ruiz", "1061000001", "andres.munoz@demo.urbanmovil.co", "3001000001"),
            conductor("Laura Marcela Idrobo Paz", "1061000002", "laura.idrobo@demo.urbanmovil.co", "3001000002"),
            conductor("Carlos Eduardo Chicangana López", "1061000003", "carlos.chicangana@demo.urbanmovil.co", "3001000003"),
            conductor("Diana Patricia Velasco Ordóñez", "1061000004", "diana.velasco@demo.urbanmovil.co", "3001000004")
                .apply { marcarNoDisponible(MotivoNoDisponible.VACACIONES, "Vacaciones programadas", ahora) },
        )
        conductorRepository.saveAll(conductores)
        log.info("Perfil demo: {} conductores sembrados (contraseña {})", conductores.size, CONTRASENA_CONDUCTORES)
    }

    private fun sembrarVehiculos() {
        if (vehiculoRepository.count() > 0) return
        val ahora = clock.instant()

        fun vehiculo(placa: String, numeroInterno: String) =
            Vehiculo(placa = placa, numeroInterno = numeroInterno, creadoEn = ahora, actualizadoEn = ahora)

        val vehiculos = listOf(
            vehiculo("SXA104", "UM-104"),
            vehiculo("SXA118", "UM-118"),
            vehiculo("SXB221", "UM-221"),
            vehiculo("SXB305", "UM-305"),
            vehiculo("SXC410", "UM-410").apply { desactivar("Mantenimiento de frenos", ahora) },
        )
        vehiculoRepository.saveAll(vehiculos)
        log.info("Perfil demo: {} vehículos sembrados", vehiculos.size)
    }

    private fun sembrarRutas() {
        if (rutaRepository.count() > 0) return
        val ahora = clock.instant()
        RutasDemo.rutas.forEach { demo ->
            val ruta = Ruta(
                codigo = demo.codigo,
                nombre = demo.nombre,
                color = demo.color,
                recorridoPrincipal = demo.recorrido,
                creadoEn = ahora,
                actualizadoEn = ahora,
            )
            demo.alternos.forEach { ruta.alternos.add(CaminoAlterno(ruta, it.nombre, it.puntos)) }
            rutaRepository.save(ruta)
        }
        log.info("Perfil demo: {} rutas sembradas", RutasDemo.rutas.size)
    }

    // ------------------------------------------------------------------ Fase 3

    /** Conductores 1, 2 y 3 de la semilla (por identificación), o null si ya no existen. */
    private fun conductorDemo(numero: Int): Conductor? =
        conductorRepository.findAll().firstOrNull { it.identificacion == "106100000$numero" }

    private fun rutaDemo(codigo: String) = rutaRepository.findAll().firstOrNull { it.codigo == codigo }

    private fun vehiculoDemo(numeroInterno: String) = vehiculoRepository.findAll().firstOrNull { it.numeroInterno == numeroInterno }

    /** Programación de hoy: conductor 1 → [R1, R2]; conductor 2 → [R2]; conductor 3 → [R1]. */
    private fun sembrarProgramacion() {
        if (programacionRepository.count() > 0) return
        val r1 = rutaDemo("R1") ?: return
        val r2 = rutaDemo("R2") ?: return
        val plan = listOf(1 to listOf(r1, r2), 2 to listOf(r2), 3 to listOf(r1))
        plan.forEach { (numero, rutas) ->
            val conductor = conductorDemo(numero) ?: return@forEach
            programacionService.reemplazar(conductor.id!!, null, rutas.map { it.id!! })
        }
        log.info("Perfil demo: programación de hoy sembrada")
    }

    /** 2 recorridos ACTIVO iniciados hace unos minutos: conductor 1 en R1 (UM-104) y conductor 2 en R2 (UM-305). */
    private fun sembrarRecorridos() {
        if (recorridoRepository.count() > 0) return
        val ahora = clock.instant()
        val plan = listOf(
            Triple(1, "R1", "UM-104") to Duration.ofMinutes(6),
            Triple(2, "R2", "UM-305") to Duration.ofMinutes(2),
        )
        plan.forEach { (datos, hace) ->
            val (numero, codigo, numeroInterno) = datos
            val conductor = conductorDemo(numero) ?: return@forEach
            val ruta = rutaDemo(codigo) ?: return@forEach
            val vehiculo = vehiculoDemo(numeroInterno) ?: return@forEach
            // Se verifica antes de llamar: una excepción dentro de la transacción la marcaría
            // como rollback-only y la app no arrancaría (p. ej. programación de otro día en el volumen).
            val enProgramacion = programacionService.deHoy(conductor.id!!)?.entrada(ruta.id!!)
            if (!conductor.disponible || !vehiculo.activo || enProgramacion == null || enProgramacion.retiroPendiente) {
                log.warn("Perfil demo: no se inicia el recorrido de {} en {} (no está programado hoy)", conductor.nombre, codigo)
                return@forEach
            }
            demoRecorridoService.iniciarDesde(
                IniciarRecorridoRequest(conductor.id, ruta.id, vehiculo.id),
                ahora.minus(hace),
            )
        }
        log.info("Perfil demo: recorridos activos sembrados")
    }

    /** Reasignación de ejemplo del administrador inicial: Carlos (conductor 3) cubre la R2. */
    private fun sembrarReasignacion() {
        if (reasignacionRepository.count() > 0) return
        val administrador = administradorRepository.findByCorreoIgnoreCase(Texto.normalizarCorreo(propiedades.adminInicial.correo))
            ?: administradorRepository.findAll().firstOrNull() ?: return
        val carlos = conductorDemo(3) ?: return
        val r2 = rutaDemo("R2") ?: return
        if (programacionService.deHoy(carlos.id!!)?.tiene(r2.id!!) == true) return
        reasignacionService.reasignar(
            carlos.id!!,
            ReasignacionRequest(motivo = MOTIVO_REASIGNACION, rutasAsignar = listOf(r2.id!!)),
            administrador,
        )
        log.info("Perfil demo: reasignación de ejemplo sembrada")
    }

    companion object {
        const val CONTRASENA_CONDUCTORES = "Conductor123"
        const val MOTIVO_REASIGNACION = "Diana reportó falla mecánica; Carlos cubre su R2"
    }
}
