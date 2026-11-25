import type { QueryClient } from '@tanstack/react-query';

// Prefijos de query keys compartidos entre features (FRONTEND-FASE-3 §2).
export const PREFIJO = {
  conductores: ['conductores'],
  vehiculos: ['vehiculos'],
  rutas: ['rutas'],
  ruta: ['ruta'],
  programaciones: ['programaciones'],
  programacion: ['programacion'],
  reasignaciones: ['reasignaciones'],
  monitoreo: ['monitoreo'],
  demoRecorridos: ['demo', 'recorridos'],
} as const;

function invalidar(qc: QueryClient, prefijos: readonly (readonly string[])[]) {
  return Promise.all(prefijos.map((queryKey) => qc.invalidateQueries({ queryKey })));
}

/**
 * Tras guardar una programación. También cambian «rutas de hoy» de Conductores y «asignada hoy a» de Rutas.
 * `incluirDetalle` = false cuando el detalle ya se actualizó con la respuesta (evita otro GET).
 */
export function invalidarTrasProgramacion(qc: QueryClient, { incluirDetalle = true } = {}) {
  return invalidar(qc, [
    PREFIJO.programaciones,
    ...(incluirDetalle ? [PREFIJO.programacion] : []),
    PREFIJO.conductores,
    PREFIJO.rutas,
  ]);
}

/** Tras reasignar: lo de la programación más el historial. El detalle llega en la respuesta del POST. */
export function invalidarTrasReasignacion(qc: QueryClient) {
  return Promise.all([invalidarTrasProgramacion(qc, { incluirDetalle: false }), invalidar(qc, [PREFIJO.reasignaciones])]);
}

/** Tras iniciar o finalizar un recorrido: cambian `enRecorrido`, `recorridosActivos`, estados y el monitoreo. */
export function invalidarTrasRecorrido(qc: QueryClient) {
  return invalidar(qc, [
    PREFIJO.programaciones,
    PREFIJO.programacion,
    PREFIJO.monitoreo,
    PREFIJO.demoRecorridos,
    PREFIJO.conductores,
    PREFIJO.vehiculos,
    PREFIJO.rutas,
    PREFIJO.ruta,
  ]);
}

/** Tras cambiar disponibilidad o el alta/baja de un conductor: el resumen de programaciones lo muestra. */
export function invalidarTrasConductor(qc: QueryClient) {
  return invalidar(qc, [PREFIJO.conductores, PREFIJO.programaciones]);
}
