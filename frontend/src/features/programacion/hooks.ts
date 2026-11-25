import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  guardarProgramacion,
  listarProgramaciones,
  listarReasignaciones,
  obtenerProgramacion,
  reasignar,
  type FiltrosReasignaciones,
} from '../../api/programacion';
import type { ReasignacionRequest } from '../../api/tipos';
import { invalidarTrasProgramacion, invalidarTrasReasignacion } from '../../app/invalidaciones';
import { hoyBogota } from '../../utils/fechas';

export const claveProgramaciones = (fecha: string) => ['programaciones', fecha] as const;
export const claveProgramacion = (conductorId: number, fecha: string) => ['programacion', conductorId, fecha] as const;
export const claveReasignaciones = (filtros: FiltrosReasignaciones) => ['reasignaciones', filtros] as const;

export function useProgramaciones(fecha: string) {
  return useQuery({
    queryKey: claveProgramaciones(fecha),
    queryFn: ({ signal }) => listarProgramaciones(fecha, signal),
    placeholderData: keepPreviousData,
  });
}

export function useProgramacion(conductorId: number | null, fecha: string) {
  return useQuery({
    queryKey: claveProgramacion(conductorId ?? -1, fecha),
    queryFn: ({ signal }) => obtenerProgramacion(conductorId as number, fecha, signal),
    enabled: conductorId !== null,
  });
}

export function useReasignaciones(filtros: FiltrosReasignaciones, habilitado = true) {
  return useQuery({
    queryKey: claveReasignaciones(filtros),
    queryFn: ({ signal }) => listarReasignaciones(filtros, signal),
    enabled: habilitado,
  });
}

export function useGuardarProgramacion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ conductorId, fecha, rutas }: { conductorId: number; fecha: string; rutas: number[] }) =>
      guardarProgramacion(conductorId, fecha, { rutas }),
    onSuccess: async (programacion) => {
      qc.setQueryData(claveProgramacion(programacion.conductorId, programacion.fecha), programacion);
      await invalidarTrasProgramacion(qc, { incluirDetalle: false });
    },
  });
}

export function useReasignar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ conductorId, datos }: { conductorId: number; datos: ReasignacionRequest }) =>
      reasignar(conductorId, datos),
    onSuccess: async (reasignacion, { conductorId }) => {
      // La respuesta trae la programación resultante: se usa tal cual, sin otro GET.
      if (reasignacion.programacion) {
        qc.setQueryData(claveProgramacion(conductorId, reasignacion.programacion.fecha), reasignacion.programacion);
      } else {
        await qc.invalidateQueries({ queryKey: claveProgramacion(conductorId, hoyBogota()) });
      }
      await invalidarTrasReasignacion(qc);
    },
  });
}
