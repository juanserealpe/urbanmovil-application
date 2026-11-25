// Tipos del contrato de la API (FRONTEND-API.md §2 y §4).
// Se copian TODOS, aunque algunos se usen en fases posteriores.

// ---------- Errores ----------
export type CodigoError =
  | 'VALIDACION' // 400
  | 'NO_AUTENTICADO' // 401: sin sesión o sesión vencida → ir al login
  | 'CREDENCIALES_INVALIDAS' // 401: solo en el login
  | 'ACCESO_DENEGADO' // 403
  | 'NO_ENCONTRADO' // 404
  | 'DATO_REPETIDO' // 409: valores únicos ya registrados (vienen en `campos`)
  | 'REGLA_NEGOCIO' // 409: una regla impide la acción
  | 'METODO_NO_PERMITIDO' // 405
  | 'ERROR_INTERNO'; // 500

export interface CampoError {
  campo: string;
  mensaje: string;
}

export interface ErrorResponse {
  codigo: CodigoError;
  mensaje: string;
  campos: CampoError[];
}

// ---------- Comunes ----------
export interface Punto {
  lat: number;
  lng: number;
}

export interface EnRecorrido {
  recorridoId: number;
  rutaCodigo: string;
}

// ---------- Auth ----------
export interface LoginRequest {
  correo: string;
  contrasena: string;
}

export interface Administrador {
  id: number;
  nombre: string;
  correo: string;
}

// ---------- Conductores ----------
export type MotivoNoDisponible = 'INCAPACIDAD' | 'VACACIONES' | 'PERMISO' | 'OTRO';

export interface Conductor {
  id: number;
  nombre: string;
  identificacion: string;
  correo: string;
  telefono: string;
  disponible: boolean;
  motivoNoDisponible: MotivoNoDisponible | null;
  detalleMotivo: string | null;
  enRecorrido: EnRecorrido | null;
  rutasHoy: string[];
}

export interface ConductorCrearRequest {
  nombre: string;
  identificacion: string;
  correo: string;
  telefono: string;
  contrasena: string;
}

export interface ConductorModificarRequest {
  nombre: string;
  identificacion: string;
  correo: string;
  telefono: string;
  contrasena?: string | null;
}

export interface DisponibilidadRequest {
  disponible: boolean;
  motivo?: MotivoNoDisponible | null;
  detalle?: string | null;
}

// ---------- Vehículos ----------
export interface Vehiculo {
  id: number;
  placa: string;
  numeroInterno: string;
  activo: boolean;
  motivoInactivacion: string | null;
  fechaInactivacion: string | null;
  enRecorrido: EnRecorrido | null;
}

export interface VehiculoRequest {
  placa: string;
  numeroInterno: string;
}

export interface EstadoVehiculoRequest {
  activo: boolean;
  motivo?: string | null;
}

// ---------- Rutas ----------
export interface CaminoAlterno {
  id: number;
  nombre: string;
  puntos: Punto[];
}

export interface RutaResumen {
  id: number;
  codigo: string;
  nombre: string;
  color: string;
  puntosRecorrido: number;
  numAlternos: number;
  recorridosActivos: number;
  conductoresHoy: string[];
  actualizadoEn: string;
}

export interface RutaDetalle extends RutaResumen {
  recorridoPrincipal: Punto[];
  inicio: Punto | null;
  final: Punto | null;
  alternos: CaminoAlterno[];
}

export interface CaminoAlternoRequest {
  id?: number | null;
  nombre: string;
  puntos: Punto[];
}

export interface RutaRequest {
  codigo: string;
  nombre: string;
  color?: string | null;
  recorridoPrincipal: Punto[];
  alternos?: CaminoAlternoRequest[];
}

export interface RutaPublica {
  codigo: string;
  nombre: string;
  color: string;
  recorridoPrincipal: Punto[];
  alternos: { nombre: string; puntos: Punto[] }[];
}

// ---------- Programación ----------
export type EstadoRutaProgramada = 'PENDIENTE' | 'EN_RECORRIDO' | 'RETIRO_PENDIENTE';

export interface RutaProgramada {
  rutaId: number;
  codigo: string;
  nombre: string;
  color: string;
  orden: number;
  estado: EstadoRutaProgramada;
}

export interface Programacion {
  conductorId: number;
  fecha: string;
  rutas: RutaProgramada[];
}

export interface ProgramacionRequest {
  rutas: number[];
}

export interface ProgramacionResumen {
  conductorId: number;
  nombre: string;
  disponible: boolean;
  enRecorrido: EnRecorrido | null;
  totalRutas: number;
}

// ---------- Reasignaciones ----------
export interface PersonaResumen {
  id: number;
  nombre: string;
}

export interface ReasignacionRequest {
  motivo: string;
  rutasAsignar?: number[];
  rutasQuitar?: number[];
}

export interface Reasignacion {
  id: number;
  fechaHora: string;
  conductor: PersonaResumen;
  administrador: PersonaResumen;
  motivo: string;
  rutasAsignadas: string[];
  rutasQuitadas: string[];
  programacion: Programacion | null;
}

// ---------- Monitoreo ----------
export interface BusMonitoreo {
  recorridoId: number;
  vehiculo: { id: number; numeroInterno: string; placa: string };
  ruta: { id: number; codigo: string; nombre: string; color: string };
  conductor: { id: number; nombre: string };
  posicion: Punto;
  horaPosicion: string;
  /** Inicio del recorrido: con `velocidadKmh` la web anima el bus entre consultas. */
  inicio: string;
}

export interface Monitoreo {
  simulado: boolean;
  generadoEn: string;
  velocidadKmh: number;
  buses: BusMonitoreo[];
}

// ---------- Demo (solo perfil demo) ----------
export type EstadoRecorrido = 'ACTIVO' | 'FINALIZADO';

export interface IniciarRecorridoRequest {
  conductorId: number;
  rutaId: number;
  vehiculoId: number;
}

export interface RecorridoDemo {
  id: number;
  conductor: { id: number; nombre: string };
  ruta: { id: number; codigo: string; nombre: string };
  vehiculo: { id: number; numeroInterno: string; placa: string };
  estado: EstadoRecorrido;
  inicio: string;
  fin: string | null;
}
