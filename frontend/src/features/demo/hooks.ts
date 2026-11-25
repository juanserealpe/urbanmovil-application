import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../api/client';
import { finalizarRecorridoDemo, iniciarRecorridoDemo, listarRecorridosDemo } from '../../api/demo';
import type { EstadoRecorrido, IniciarRecorridoRequest } from '../../api/tipos';
import { invalidarTrasRecorrido } from '../../app/invalidaciones';

export const claveRecorridosDemo = (estado: EstadoRecorrido) => ['demo', 'recorridos', estado] as const;

export function useRecorridosDemo(estado: EstadoRecorrido) {
  return useQuery({
    queryKey: claveRecorridosDemo(estado),
    queryFn: ({ signal }) => listarRecorridosDemo(estado, signal),
    placeholderData: keepPreviousData,
  });
}

/** `desconocido`: falló por otra causa (red, 500); no se puede saber si hay perfil demo. */
export type ModoDemo = 'cargando' | 'activo' | 'inactivo' | 'desconocido';

/**
 * ¿El backend corre con el perfil `demo`? Sin él, /demo/** responde 404.
 * Comparte la query de los recorridos activos, así que no hace una petición extra.
 */
export function useModoDemo(): { modo: ModoDemo; error: unknown; reintentar: () => void } {
  const consulta = useRecorridosDemo('ACTIVO');
  const reintentar = () => void consulta.refetch();
  let modo: ModoDemo = 'cargando';
  if (consulta.data) modo = 'activo';
  else if (consulta.error instanceof ApiError && consulta.error.status === 404) modo = 'inactivo';
  else if (consulta.isError) modo = 'desconocido';
  return { modo, error: consulta.error, reintentar };
}

export function useIniciarRecorrido() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (datos: IniciarRecorridoRequest) => iniciarRecorridoDemo(datos),
    onSuccess: () => invalidarTrasRecorrido(qc),
  });
}

export function useFinalizarRecorrido() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => finalizarRecorridoDemo(id),
    onSuccess: () => invalidarTrasRecorrido(qc),
  });
}
