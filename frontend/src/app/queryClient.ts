import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/client';

function esError4xx(error: unknown) {
  return error instanceof ApiError && error.status >= 400 && error.status < 500;
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Sin reintentos para 4xx; un reintento para errores de red o 5xx.
      retry: (intentos, error) => !esError4xx(error) && intentos < 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
      // API local: no pausar si el navegador cree estar sin red.
      networkMode: 'always',
    },
    mutations: { retry: false, networkMode: 'always' },
  },
});
