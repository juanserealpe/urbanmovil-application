import { z } from 'zod';
import { ApiError } from '../../../api/client';
import type { RutaRequest } from '../../../api/tipos';
import { esColorHex, MAX_ALTERNOS, MAX_PUNTOS_TRAZADO } from '../constantes';
import type { EstadoTrazado } from './trazado';

// Mismos mensajes que el backend (FRONTEND-API.md §5.4).

export const esquemaDatos = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, 'Escriba el nombre de la ruta')
    .max(120, 'El nombre admite máximo 120 caracteres'),
  codigo: z
    .string()
    .trim()
    .min(1, 'Escriba el código de la ruta')
    .max(10, 'El código admite máximo 10 caracteres')
    .transform((c) => c.toUpperCase()),
  color: z
    .string()
    .trim()
    .refine((c) => c === '' || esColorHex(c), 'Color inválido')
    .transform((c) => c.toUpperCase()),
});

export type EntradaDatos = z.input<typeof esquemaDatos>;
export type SalidaDatos = z.output<typeof esquemaDatos>;
export const CAMPOS_DATOS = ['nombre', 'codigo', 'color'] as const;

const MSJ_COORDENADAS = 'Coordenadas inválidas';
const punto = z.object({
  lat: z.number(MSJ_COORDENADAS).min(-90, MSJ_COORDENADAS).max(90, MSJ_COORDENADAS),
  lng: z.number(MSJ_COORDENADAS).min(-180, MSJ_COORDENADAS).max(180, MSJ_COORDENADAS),
});
const MSJ_MAX_PUNTOS = 'El trazado supera el máximo de puntos permitido';

const esquemaTrazado = z.object({
  // El orden de los .min importa: con 0 puntos el primer mensaje es «Falta trazar…».
  recorridoPrincipal: z
    .array(punto)
    .min(1, 'Falta trazar el recorrido principal')
    .min(2, 'La ruta debe tener punto de inicio y punto final')
    .max(MAX_PUNTOS_TRAZADO, MSJ_MAX_PUNTOS),
  alternos: z
    .array(
      z.object({
        nombre: z
          .string()
          .trim()
          .min(1, 'Escriba el nombre del camino alterno')
          .max(120, 'El nombre admite máximo 120 caracteres'),
        puntos: z
          .array(punto)
          .min(2, 'Cada camino alterno debe tener al menos dos puntos')
          .max(MAX_PUNTOS_TRAZADO, MSJ_MAX_PUNTOS),
      }),
    )
    .max(MAX_ALTERNOS, 'Una ruta admite máximo 10 caminos alternos'),
});

// ---------- Errores de trazado (cliente y backend comparten forma) ----------

export interface ErroresAlterno {
  nombre?: string;
  puntos?: string;
  otro?: string;
}

export interface ErroresTrazado {
  principal?: string;
  /** Por clave local del alterno. */
  alternos: Record<string, ErroresAlterno>;
  /** Errores sobre la lista completa (`alternos`). */
  listaAlternos?: string;
  /** Resumen para la alerta general (solo validación del cliente; los del backend van a `root.servidor`). */
  resumen?: string;
}

export const SIN_ERRORES: ErroresTrazado = { alternos: {} };

/** Convierte un path de zod al formato de campo del backend: `alternos[1].puntos[3].lat`. */
function aNombreCampo(path: readonly PropertyKey[]): string {
  return path.reduce<string>(
    (acc, parte) => (typeof parte === 'number' ? `${acc}[${parte}]` : acc ? `${acc}.${String(parte)}` : String(parte)),
    '',
  );
}

const CAMPO_ALTERNO = /^alternos\[(\d+)\]\.(nombre|puntos|id)/;

/**
 * Ubica un mensaje según el nombre de campo del backend. `claves` traduce el índice `i`
 * (posición en la lista enviada) a la clave local del alterno. Gana el primer mensaje de cada lugar.
 * Devuelve false si el campo no es de trazado.
 */
function ubicar(errores: ErroresTrazado, campo: string, mensaje: string, claves: readonly string[]): boolean {
  if (campo === 'recorridoPrincipal' || campo.startsWith('recorridoPrincipal[')) {
    errores.principal ??= mensaje;
    return true;
  }
  const m = CAMPO_ALTERNO.exec(campo);
  if (m) {
    const clave = claves[Number(m[1])];
    if (clave === undefined) {
      errores.listaAlternos ??= mensaje;
      return true;
    }
    const lugar: keyof ErroresAlterno = m[2] === 'id' ? 'otro' : (m[2] as 'nombre' | 'puntos');
    const actuales = (errores.alternos[clave] ??= {});
    actuales[lugar] ??= mensaje;
    return true;
  }
  if (campo === 'alternos' || campo.startsWith('alternos')) {
    errores.listaAlternos ??= mensaje;
    return true;
  }
  return false;
}

export function hayErroresTrazado(e: ErroresTrazado): boolean {
  return Boolean(e.principal || e.listaAlternos || Object.keys(e.alternos).length > 0);
}

/** Validación del trazado antes de enviar. null = todo bien. */
export function validarTrazado(estado: EstadoTrazado): ErroresTrazado | null {
  const r = esquemaTrazado.safeParse({
    recorridoPrincipal: estado.principal,
    alternos: estado.alternos.map((a) => ({ nombre: a.nombre, puntos: a.puntos })),
  });
  if (r.success) return null;

  const claves = estado.alternos.map((a) => a.clave);
  const errores: ErroresTrazado = { alternos: {} };
  const mensajes = new Set<string>();
  for (const issue of r.error.issues) {
    if (ubicar(errores, aNombreCampo(issue.path), issue.message, claves)) mensajes.add(issue.message);
  }
  errores.resumen = [...mensajes].join(' ');
  return errores;
}

/** Errores de trazado que devolvió el backend (`recorridoPrincipal`, `alternos[i].…`). */
export function erroresTrazadoDeApi(error: unknown, claves: readonly string[]): ErroresTrazado | null {
  if (!(error instanceof ApiError)) return null;
  const errores: ErroresTrazado = { alternos: {} };
  for (const { campo, mensaje } of error.body.campos) ubicar(errores, campo, mensaje, claves);
  return hayErroresTrazado(errores) ? errores : null;
}

/** Cuerpo del POST/PUT. En el PUT van SIEMPRE todos los alternos: con `id` los existentes, sin `id` los nuevos. */
export function construirCuerpo(datos: SalidaDatos, estado: EstadoTrazado): RutaRequest {
  return {
    codigo: datos.codigo,
    nombre: datos.nombre,
    // Vacío: no se envía (al crear lo asigna el backend; al editar se conserva).
    ...(datos.color ? { color: datos.color } : {}),
    recorridoPrincipal: estado.principal,
    alternos: estado.alternos.map((a) => ({
      ...(a.id !== undefined ? { id: a.id } : {}),
      nombre: a.nombre.trim(),
      puntos: a.puntos,
    })),
  };
}
