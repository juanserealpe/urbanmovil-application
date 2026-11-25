import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { login, logout, me } from '../api/auth';
import { ApiError } from '../api/client';
import type { LoginRequest } from '../api/tipos';

export const CLAVE_ME = ['me'] as const;

/** Estado que viaja en `location.state` hacia /login para volver a la ruta de origen. */
export interface EstadoLogin {
  desde?: string;
}

export function leerDesde(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'desde' in state) {
    const desde = (state as EstadoLogin).desde;
    if (typeof desde === 'string' && desde.startsWith('/') && !desde.startsWith('/login')) return desde;
  }
  return '/';
}

export function useSesion() {
  const query = useQuery({
    queryKey: CLAVE_ME,
    queryFn: ({ signal }) => me(signal),
    retry: false,
    staleTime: Infinity,
  });
  const sinSesion = query.error instanceof ApiError && query.error.status === 401;
  return {
    administrador: query.data,
    cargando: query.isPending,
    sinSesion,
    error: query.error,
    reintentar: query.refetch,
  };
}

export function useLogin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (datos: LoginRequest) => login(datos),
    // El login ya devuelve el Administrador: se escribe directo en ['me'] sin otra petición.
    onSuccess: (administrador) => qc.setQueryData(CLAVE_ME, administrador),
  });
}

export function useCerrarSesion() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  return useMutation({
    mutationFn: () => logout(),
    onSettled: () => {
      // Aunque el logout falle (p. ej. la sesión ya había vencido), el usuario sale.
      qc.clear();
      navigate('/login', { replace: true });
    },
  });
}
