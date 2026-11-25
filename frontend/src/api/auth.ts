import { api } from './client';
import type { Administrador, LoginRequest } from './tipos';

export const login = (datos: LoginRequest) =>
  api<Administrador>('/auth/login', { method: 'POST', body: datos });

export const logout = () => api<void>('/auth/logout', { method: 'POST' });

export const me = (signal?: AbortSignal) => api<Administrador>('/auth/me', { signal });
