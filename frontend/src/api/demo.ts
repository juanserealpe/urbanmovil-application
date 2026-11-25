import { api, queryString } from './client';
import type { EstadoRecorrido, IniciarRecorridoRequest, RecorridoDemo } from './tipos';

// Solo existen con el perfil `demo` del backend; sin él, todo /demo/** responde 404.

export const listarRecorridosDemo = (estado: EstadoRecorrido | undefined, signal?: AbortSignal) =>
  api<RecorridoDemo[]>(`/demo/recorridos${queryString({ estado })}`, { signal });

export const iniciarRecorridoDemo = (datos: IniciarRecorridoRequest) =>
  api<RecorridoDemo>('/demo/recorridos', { method: 'POST', body: datos });

export const finalizarRecorridoDemo = (id: number) =>
  api<RecorridoDemo>(`/demo/recorridos/${id}/finalizar`, { method: 'POST' });
