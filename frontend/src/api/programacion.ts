import { api, queryString } from './client';
import type { Programacion, ProgramacionRequest, ProgramacionResumen, Reasignacion, ReasignacionRequest } from './tipos';

export const listarProgramaciones = (fecha: string, signal?: AbortSignal) =>
  api<ProgramacionResumen[]>(`/programaciones${queryString({ fecha })}`, { signal });

export const obtenerProgramacion = (conductorId: number, fecha: string, signal?: AbortSignal) =>
  api<Programacion>(`/conductores/${conductorId}/programacion${queryString({ fecha })}`, { signal });

/** `rutas`: lista COMPLETA y ORDENADA de ids. Las que no vienen se quitan. */
export const guardarProgramacion = (conductorId: number, fecha: string, datos: ProgramacionRequest) =>
  api<Programacion>(`/conductores/${conductorId}/programacion${queryString({ fecha })}`, {
    method: 'PUT',
    body: datos,
  });

export interface FiltrosReasignaciones {
  conductorId?: number;
  fecha?: string;
}

export const listarReasignaciones = (filtros: FiltrosReasignaciones, signal?: AbortSignal) =>
  api<Reasignacion[]>(`/reasignaciones${queryString({ conductorId: filtros.conductorId, fecha: filtros.fecha })}`, {
    signal,
  });

/** Siempre sobre la programación de HOY. */
export const reasignar = (conductorId: number, datos: ReasignacionRequest) =>
  api<Reasignacion>(`/conductores/${conductorId}/reasignaciones`, { method: 'POST', body: datos });
