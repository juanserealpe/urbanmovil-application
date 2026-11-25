import type { Punto } from '../api/tipos';

/**
 * Estilo de la ciudad ficticia, servido por el backend (`GET /api/v1/public/mapa/estilo`): el MISMO
 * que carga la app del pasajero, así el mapa se ve igual en las dos. Trae las calles embebidas, no
 * depende de un servidor de teselas. URL absoluta porque MapLibre la resuelve también desde su worker.
 */
export const ESTILO_MAPA = `${globalThis.location?.origin ?? ''}/api/v1/public/mapa/estilo`;
/** Parque Central de la ciudad ficticia (esquina Av. Central con Av. Bolívar). */
export const CENTRO_INICIAL: Punto = { lat: 2.444, lng: -76.606 };
export const ZOOM_INICIAL = 15;
