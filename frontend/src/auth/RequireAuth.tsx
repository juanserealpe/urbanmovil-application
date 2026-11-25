import { Navigate, Outlet, useLocation } from 'react-router';
import { CargadorPantalla } from '../components/CargadorPantalla';
import { ErrorPantalla } from '../components/ErrorPantalla';
import { mensajeDeError, MENSAJE_INESPERADO } from '../utils/errores';
import { useSesion, type EstadoLogin } from './useSesion';

export function RequireAuth() {
  const { administrador, cargando, sinSesion, error, reintentar } = useSesion();
  const location = useLocation();

  if (administrador) return <Outlet />;
  if (cargando) return <CargadorPantalla />;
  if (sinSesion) {
    const state: EstadoLogin = { desde: `${location.pathname}${location.search}` };
    return <Navigate to="/login" replace state={state} />;
  }
  return (
    <ErrorPantalla
      mensaje={mensajeDeError(error) ?? MENSAJE_INESPERADO}
      onReintentar={() => void reintentar()}
    />
  );
}
