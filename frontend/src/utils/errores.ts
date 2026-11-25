import { notifications } from '@mantine/notifications';
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { ApiError } from '../api/client';

export const MENSAJE_INESPERADO = 'Ocurrió un error inesperado. Intente de nuevo.';

/** Clave de react-hook-form donde se guarda el error general del formulario. */
export const ERROR_GENERAL = 'root.servidor' as const;

export function notificarError(mensaje: string) {
  notifications.show({ color: 'red', title: 'No se pudo completar', message: mensaje });
}

export function notificarExito(mensaje: string, detalle?: string) {
  notifications.show({ color: 'teal', title: mensaje, message: detalle ?? '' });
}

/**
 * Mensaje que se le muestra al usuario para un error que no es de formulario
 * (eliminar, reactivar…). Devuelve null si no hay que mostrar nada.
 */
export function mensajeDeError(error: unknown): string | null {
  if (!(error instanceof ApiError)) return MENSAJE_INESPERADO;
  switch (error.codigo) {
    case 'NO_AUTENTICADO':
      return null; // el listener de `sesion-expirada` ya se encarga
    case 'ERROR_INTERNO':
      return error.sinConexion ? error.message : MENSAJE_INESPERADO;
    case 'METODO_NO_PERMITIDO':
      return MENSAJE_INESPERADO;
    default:
      // Sin formulario donde ubicarlos, los mensajes por campo son más útiles que el general.
      return error.body.campos.length > 0 ? error.body.campos.map((c) => c.mensaje).join(' ') : error.message;
  }
}

/** Para acciones sin formulario: muestra el `mensaje` del backend en una notificación roja. */
export function notificarErrorAccion(error: unknown) {
  const mensaje = mensajeDeError(error);
  if (mensaje) notificarError(mensaje);
}

/**
 * Lleva un error de la API a un formulario de react-hook-form (FRONTEND-FASE-1 §5.5):
 * 1. Con `campos`: cada mensaje bajo su input; los que no existen en el formulario van al error general.
 * 2. VALIDACION / DATO_REPETIDO / CREDENCIALES_INVALIDAS sin `campos`: error general (Alert roja).
 * 3. REGLA_NEGOCIO (y NO_ENCONTRADO / ACCESO_DENEGADO): notificación roja con `mensaje`.
 * 4. ERROR_INTERNO u otro: notificación roja genérica (o la de «sin conexión»).
 */
export function manejarErrorFormulario<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  camposFormulario: readonly Path<T>[],
) {
  if (!(error instanceof ApiError)) {
    notificarError(MENSAJE_INESPERADO);
    return;
  }

  const { codigo, campos, mensaje } = error.body;

  if (codigo === 'NO_AUTENTICADO') return;

  if ((codigo === 'VALIDACION' || codigo === 'DATO_REPETIDO') && campos.length > 0) {
    const sueltos: string[] = [];
    for (const { campo, mensaje: msj } of campos) {
      const enFormulario = camposFormulario.find((c) => c === campo);
      if (enFormulario) setError(enFormulario, { type: 'servidor', message: msj });
      else sueltos.push(msj);
    }
    if (sueltos.length > 0) setError(ERROR_GENERAL, { type: 'servidor', message: sueltos.join(' ') });
    return;
  }

  if (codigo === 'VALIDACION' || codigo === 'DATO_REPETIDO' || codigo === 'CREDENCIALES_INVALIDAS') {
    setError(ERROR_GENERAL, { type: 'servidor', message: mensaje });
    return;
  }

  notificarErrorAccion(error);
}
