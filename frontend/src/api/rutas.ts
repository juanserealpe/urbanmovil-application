import { api, queryString } from './client';
import type { RutaDetalle, RutaRequest, RutaResumen } from './tipos';

export const listarRutas = (q: string | undefined, signal?: AbortSignal) =>
  api<RutaResumen[]>(`/rutas${queryString({ q })}`, { signal });

export const obtenerRuta = (id: number, signal?: AbortSignal) => api<RutaDetalle>(`/rutas/${id}`, { signal });

export const crearRuta = (datos: RutaRequest) => api<RutaDetalle>('/rutas', { method: 'POST', body: datos });

/** Reemplazo completo: los alternos que no vengan en `datos.alternos` se eliminan. */
export const modificarRuta = (id: number, datos: RutaRequest) =>
  api<RutaDetalle>(`/rutas/${id}`, { method: 'PUT', body: datos });

export const eliminarRuta = (id: number) => api<void>(`/rutas/${id}`, { method: 'DELETE' });
