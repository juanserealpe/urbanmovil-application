import { useDocumentVisibility } from '@mantine/hooks';
import { useQueries, useQuery, type UseQueryResult } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { obtenerMonitoreo } from '../../api/monitoreo';
import { obtenerRuta } from '../../api/rutas';
import type { RutaDetalle } from '../../api/tipos';
import { claveRuta } from '../rutas/hooks';

export const CLAVE_MONITOREO = ['monitoreo'] as const;
const INTERVALO_MS = 4000;

/**
 * Polling cada 4 s. Se pausa con la pestaña oculta (doble seguro: `refetchIntervalInBackground`
 * y `document.visibilityState`) y al salir de la pantalla (la query deja de tener observadores).
 */
export function useMonitoreo() {
  const visibilidad = useDocumentVisibility();
  return useQuery({
    queryKey: CLAVE_MONITOREO,
    queryFn: ({ signal }) => obtenerMonitoreo(signal),
    refetchInterval: visibilidad === 'visible' ? INTERVALO_MS : false,
    refetchIntervalInBackground: false,
    // Al volver a la pestaña, ponerse al día enseguida.
    refetchOnWindowFocus: true,
    staleTime: 0,
  });
}

// Fuera del componente: `combine` estable + structural sharing = mismo array mientras no cambien los datos.
function combinarTrazados(resultados: UseQueryResult<RutaDetalle>[]) {
  return {
    rutas: resultados.flatMap((r) => (r.data ? [r.data] : [])),
    todosCargados: resultados.length > 0 && resultados.every((r) => r.isSuccess),
  };
}

/** Trazados de las rutas con buses; no cambian entre consultas. */
export function useTrazadosRutas(ids: number[]) {
  return useQueries({
    queries: ids.map((id) => ({
      queryKey: claveRuta(id),
      queryFn: ({ signal }: { signal: AbortSignal }) => obtenerRuta(id, signal),
      staleTime: Infinity,
    })),
    combine: combinarTrazados,
  });
}

const TIC_ANIMACION_MS = 100;

/**
 * Hora local (epoch ms) que se actualiza cada 100 ms para animar los buses entre consultas.
 * Con la pestaña oculta se detiene: no tiene sentido re-renderizar lo que nadie ve.
 */
export function useAhora(): number {
  const visibilidad = useDocumentVisibility();
  const [ahora, setAhora] = useState(() => Date.now());
  useEffect(() => {
    if (visibilidad !== 'visible') return;
    const id = setInterval(() => setAhora(Date.now()), TIC_ANIMACION_MS);
    return () => clearInterval(id);
  }, [visibilidad]);
  return ahora;
}
