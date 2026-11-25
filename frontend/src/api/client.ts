import type { ErrorResponse } from './tipos';

const BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1';

export const EVENTO_SESION_EXPIRADA = 'sesion-expirada';

export const MENSAJE_SIN_CONEXION =
  'No se pudo conectar con el servidor. Verifique que el backend esté encendido.';

export class ApiError extends Error {
  readonly status: number;
  readonly body: ErrorResponse;
  /** true cuando el error lo generó el cliente porque no hubo respuesta JSON del backend. */
  readonly sinConexion: boolean;

  constructor(status: number, body: ErrorResponse, sinConexion = false) {
    super(body.mensaje);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
    this.sinConexion = sinConexion;
  }

  get codigo() {
    return this.body.codigo;
  }

  /** Errores por campo, listos para un formulario: { correo: 'Ya existe...' } */
  get porCampo(): Record<string, string> {
    return Object.fromEntries(this.body.campos.map((c) => [c.campo, c.mensaje]));
  }
}

function errorSinConexion(status: number): ApiError {
  return new ApiError(
    status,
    { codigo: 'ERROR_INTERNO', mensaje: MENSAJE_SIN_CONEXION, campos: [] },
    true,
  );
}

function esErrorResponse(valor: unknown): valor is ErrorResponse {
  return (
    typeof valor === 'object' &&
    valor !== null &&
    'codigo' in valor &&
    'mensaje' in valor
  );
}

export interface OpcionesApi {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  signal?: AbortSignal;
}

export async function api<T>(path: string, { method = 'GET', body, signal }: OpcionesApi = {}): Promise<T> {
  const conCuerpo = body !== undefined;
  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      signal,
      credentials: 'include', // OBLIGATORIO: envía la cookie JSESSIONID
      headers: conCuerpo ? { 'Content-Type': 'application/json' } : undefined,
      body: conCuerpo ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    // Una cancelación de React Query no es un error de conexión.
    if (e instanceof DOMException && e.name === 'AbortError') throw e;
    throw errorSinConexion(0);
  }

  if (res.status === 204) return undefined as T;

  const esJson = res.headers.get('content-type')?.includes('application/json') ?? false;
  const datos: unknown = esJson ? await res.json().catch(() => null) : null;

  if (!res.ok) {
    if (!esErrorResponse(datos)) throw errorSinConexion(res.status);
    const error: ErrorResponse = { ...datos, campos: datos.campos ?? [] };
    if (res.status === 401 && error.codigo === 'NO_AUTENTICADO') {
      window.dispatchEvent(new Event(EVENTO_SESION_EXPIRADA));
    }
    throw new ApiError(res.status, error);
  }

  if (!esJson) throw errorSinConexion(res.status);
  return datos as T;
}

/** Construye `?a=1&b=2` omitiendo los valores vacíos. */
export function queryString(params: Record<string, string | number | boolean | null | undefined>): string {
  const sp = new URLSearchParams();
  for (const [clave, valor] of Object.entries(params)) {
    if (valor === undefined || valor === null) continue;
    const texto = String(valor).trim();
    if (texto !== '') sp.set(clave, texto);
  }
  const s = sp.toString();
  return s ? `?${s}` : '';
}
