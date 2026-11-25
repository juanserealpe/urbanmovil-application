import { api } from './client';
import type { Monitoreo } from './tipos';

export const obtenerMonitoreo = (signal?: AbortSignal) => api<Monitoreo>('/monitoreo/buses', { signal });
