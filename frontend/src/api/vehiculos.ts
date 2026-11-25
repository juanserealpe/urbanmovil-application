import { api, queryString } from './client';
import type { EstadoVehiculoRequest, Vehiculo, VehiculoRequest } from './tipos';

export interface FiltrosVehiculos {
  q?: string;
  activo?: boolean;
}

export const listarVehiculos = (filtros: FiltrosVehiculos = {}, signal?: AbortSignal) =>
  api<Vehiculo[]>(`/vehiculos${queryString({ q: filtros.q, activo: filtros.activo })}`, { signal });

export const crearVehiculo = (datos: VehiculoRequest) =>
  api<Vehiculo>('/vehiculos', { method: 'POST', body: datos });

export const modificarVehiculo = (id: number, datos: VehiculoRequest) =>
  api<Vehiculo>(`/vehiculos/${id}`, { method: 'PUT', body: datos });

export const eliminarVehiculo = (id: number) => api<void>(`/vehiculos/${id}`, { method: 'DELETE' });

export const cambiarEstadoVehiculo = (id: number, datos: EstadoVehiculoRequest) =>
  api<Vehiculo>(`/vehiculos/${id}/estado`, { method: 'PATCH', body: datos });
