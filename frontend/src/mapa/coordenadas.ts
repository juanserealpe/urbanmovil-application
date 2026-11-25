import type { Punto } from '../api/tipos';

/**
 * ÚNICO lugar donde se convierte entre el orden de la API ({ lat, lng })
 * y el de MapLibre / GeoJSON ([lng, lat]). No inviertas coordenadas a mano en otro sitio.
 */
export function aLngLat(p: Punto): [number, number] {
  return [p.lng, p.lat];
}

export function aPunto(lngLat: { lng: number; lat: number }): Punto {
  return { lat: lngLat.lat, lng: lngLat.lng };
}
