import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cambiarDisponibilidad,
  crearConductor,
  eliminarConductor,
  listarConductores,
  modificarConductor,
  type FiltrosConductores,
} from '../../api/conductores';
import type { ConductorCrearRequest, ConductorModificarRequest, DisponibilidadRequest } from '../../api/tipos';

export const CLAVE_CONDUCTORES = ['conductores'] as const;

export function useConductores(filtros: FiltrosConductores) {
  return useQuery({
    queryKey: [...CLAVE_CONDUCTORES, filtros],
    queryFn: ({ signal }) => listarConductores(filtros, signal),
    placeholderData: keepPreviousData,
  });
}

/** Invalida por prefijo para refrescar todas las variantes de filtros. */
function useInvalidarConductores() {
  const qc = useQueryClient();
  return () => qc.invalidateQueries({ queryKey: CLAVE_CONDUCTORES });
}

export function useCrearConductor() {
  const invalidar = useInvalidarConductores();
  return useMutation({
    mutationFn: (datos: ConductorCrearRequest) => crearConductor(datos),
    onSuccess: invalidar,
  });
}

export function useModificarConductor() {
  const invalidar = useInvalidarConductores();
  return useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: ConductorModificarRequest }) => modificarConductor(id, datos),
    onSuccess: invalidar,
  });
}

export function useEliminarConductor() {
  const invalidar = useInvalidarConductores();
  return useMutation({
    mutationFn: (id: number) => eliminarConductor(id),
    onSuccess: invalidar,
  });
}

export function useCambiarDisponibilidad() {
  const invalidar = useInvalidarConductores();
  return useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: DisponibilidadRequest }) => cambiarDisponibilidad(id, datos),
    onSuccess: invalidar,
  });
}
