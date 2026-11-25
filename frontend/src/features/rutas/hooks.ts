import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { crearRuta, eliminarRuta, listarRutas, modificarRuta, obtenerRuta } from '../../api/rutas';
import type { RutaDetalle, RutaRequest } from '../../api/tipos';

export const CLAVE_RUTAS = ['rutas'] as const;
export const claveRuta = (id: number) => ['ruta', id] as const;

export function useRutas(q: string) {
  return useQuery({
    queryKey: [...CLAVE_RUTAS, { q }],
    queryFn: ({ signal }) => listarRutas(q, signal),
    placeholderData: keepPreviousData,
  });
}

interface OpcionesRuta {
  /**
   * El editor necesita la versión vigente (el PUT reemplaza todo):
   * siempre vuelve a pedirla al montar aunque haya caché.
   */
  siempreFresca?: boolean;
}

export function useRuta(id: number | null, { siempreFresca = false }: OpcionesRuta = {}) {
  return useQuery({
    queryKey: claveRuta(id ?? -1),
    queryFn: ({ signal }) => obtenerRuta(id as number, signal),
    enabled: id !== null,
    placeholderData: siempreFresca ? undefined : keepPreviousData,
    refetchOnMount: siempreFresca ? 'always' : true,
  });
}

/**
 * Tras guardar o eliminar: la lista, el detalle y los conductores
 * (su columna «rutas de hoy» muestra códigos y se limpia al eliminar una ruta).
 */
function useInvalidarRutas() {
  const qc = useQueryClient();
  return async (id: number, guardada?: RutaDetalle) => {
    if (guardada) qc.setQueryData(claveRuta(id), guardada);
    await Promise.all([
      qc.invalidateQueries({ queryKey: CLAVE_RUTAS }),
      qc.invalidateQueries({ queryKey: claveRuta(id) }),
      qc.invalidateQueries({ queryKey: ['conductores'] }),
      // Código/nombre/color se muestran en la programación; al eliminar, la ruta sale de ella.
      qc.invalidateQueries({ queryKey: ['programaciones'] }),
      qc.invalidateQueries({ queryKey: ['programacion'] }),
    ]);
  };
}

export function useGuardarRuta() {
  const invalidar = useInvalidarRutas();
  return useMutation({
    mutationFn: ({ id, datos }: { id: number | null; datos: RutaRequest }) =>
      id === null ? crearRuta(datos) : modificarRuta(id, datos),
    onSuccess: (guardada) => invalidar(guardada.id, guardada),
  });
}

export function useEliminarRuta() {
  const qc = useQueryClient();
  const invalidar = useInvalidarRutas();
  return useMutation({
    mutationFn: (id: number) => eliminarRuta(id),
    onSuccess: async (_, id) => {
      qc.removeQueries({ queryKey: claveRuta(id) });
      await invalidar(id);
    },
  });
}
