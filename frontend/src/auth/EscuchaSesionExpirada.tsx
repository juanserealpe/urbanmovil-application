import { notifications } from '@mantine/notifications';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { EVENTO_SESION_EXPIRADA } from '../api/client';
import { CLAVE_ME, type EstadoLogin } from './useSesion';

/**
 * Elemento raíz del router: escucha el evento `sesion-expirada` que dispara el cliente
 * ante un 401 NO_AUTENTICADO, limpia la caché y manda al login.
 */
export function EscuchaSesionExpirada() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const rutaActual = useRef(location.pathname + location.search);

  useEffect(() => {
    rutaActual.current = location.pathname + location.search;
  }, [location.pathname, location.search]);

  useEffect(() => {
    function alVencer() {
      // Evita el bucle: en /login el 401 de /auth/me es lo esperado.
      if (rutaActual.current.startsWith('/login')) return;
      // Si nunca hubo sesión en esta pestaña, RequireAuth redirige sin aviso.
      if (!qc.getQueryData(CLAVE_ME)) return;

      const state: EstadoLogin = { desde: rutaActual.current };
      qc.clear();
      notifications.show({
        id: 'sesion-expirada',
        color: 'orange',
        title: 'Sesión vencida',
        message: 'Su sesión venció. Inicie sesión de nuevo.',
      });
      navigate('/login', { replace: true, state });
    }

    window.addEventListener(EVENTO_SESION_EXPIRADA, alVencer);
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alVencer);
  }, [qc, navigate]);

  return <Outlet />;
}
