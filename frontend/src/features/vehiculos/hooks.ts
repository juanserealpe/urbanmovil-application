import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cambiarEstadoVehiculo,
  crearVehiculo,
  eliminarVehiculo,
  listarVehiculos,
  modificarVehiculo,
  type FiltrosVehiculos,
} from '../../api/vehiculos';
import type { EstadoVehiculoRequest, VehiculoRequest } from '../../api/tipos';

export const CLAVE_VEHICULOS = ['vehiculos'] as const;

export function useVehiculos(filtros: FiltrosVehiculos) {
  return useQuery({
    queryKey: [...CLAVE_VEHICULOS, filtros],
    queryFn: ({ signal }) => listarVehiculos(filtros, signal),
    placeholderData: keepPreviousData,
  });
}

/** Invalida por prefijo para refrescar todas las variantes de filtros. */
function useInvalidarVehiculos() {
  const qc = useQueryClient();
  return () => qc.invalidateQueries({ queryKey: CLAVE_VEHICULOS });
}

export function useCrearVehiculo() {
  const invalidar = useInvalidarVehiculos();
  return useMutation({
    mutationFn: (datos: VehiculoRequest) => crearVehiculo(datos),
    onSuccess: invalidar,
  });
}

export function useModificarVehiculo() {
  const invalidar = useInvalidarVehiculos();
  return useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: VehiculoRequest }) => modificarVehiculo(id, datos),
    onSuccess: invalidar,
  });
}

export function useEliminarVehiculo() {
  const invalidar = useInvalidarVehiculos();
  return useMutation({
    mutationFn: (id: number) => eliminarVehiculo(id),
    onSuccess: invalidar,
  });
}

export function useCambiarEstadoVehiculo() {
  const invalidar = useInvalidarVehiculos();
  return useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: EstadoVehiculoRequest }) => cambiarEstadoVehiculo(id, datos),
    onSuccess: invalidar,
  });
}
