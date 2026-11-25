import { api, queryString } from './client';
import type {
  Conductor,
  ConductorCrearRequest,
  ConductorModificarRequest,
  DisponibilidadRequest,
} from './tipos';

export interface FiltrosConductores {
  q?: string;
  disponible?: boolean;
}

export const listarConductores = (filtros: FiltrosConductores = {}, signal?: AbortSignal) =>
  api<Conductor[]>(`/conductores${queryString({ q: filtros.q, disponible: filtros.disponible })}`, {
    signal,
  });

export const crearConductor = (datos: ConductorCrearRequest) =>
  api<Conductor>('/conductores', { method: 'POST', body: datos });

export const modificarConductor = (id: number, datos: ConductorModificarRequest) =>
  api<Conductor>(`/conductores/${id}`, { method: 'PUT', body: datos });

export const eliminarConductor = (id: number) =>
  api<void>(`/conductores/${id}`, { method: 'DELETE' });

export const cambiarDisponibilidad = (id: number, datos: DisponibilidadRequest) =>
  api<Conductor>(`/conductores/${id}/disponibilidad`, { method: 'PATCH', body: datos });
