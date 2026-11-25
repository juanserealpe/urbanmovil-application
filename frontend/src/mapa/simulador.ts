import type { Punto } from '../api/tipos';

/**
 * Port 1:1 de `SimuladorPosicion.kt` (backend). El servidor da `inicio` y `velocidadKmh` y la web
 * calcula la posición en cada cuadro, así el bus se mueve suave entre consultas.
 * Los valores esperados de `simulador.test.ts` son los mismos del backend y de la app: NO tocar uno solo.
 */

const RADIO_TIERRA_METROS = 6_371_000;
const aRad = (grados: number) => (grados * Math.PI) / 180;

/** Distancia haversine en metros. */
export function distanciaMetros(a: Punto, b: Punto): number {
  const lat1 = aRad(a.lat);
  const lat2 = aRad(b.lat);
  const dLat = lat2 - lat1;
  const dLng = aRad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * RADIO_TIERRA_METROS * Math.asin(Math.sqrt(Math.min(Math.max(h, 0), 1)));
}

export function longitudMetros(puntos: Punto[]): number {
  let total = 0;
  for (let i = 1; i < puntos.length; i++) total += distanciaMetros(puntos[i - 1], puntos[i]);
  return total;
}

/** Fracción de la vuelta en curso, en [0, 1). Antes de `inicio` es 0. */
function fraccion(longitud: number, inicioMs: number, ahoraMs: number, velocidadKmh: number): number {
  if (longitud <= 0 || velocidadKmh <= 0) return 0;
  const duracionVueltaSeg = longitud / ((velocidadKmh * 1000) / 3600);
  const transcurridoSeg = Math.max(0, ahoraMs - inicioMs) / 1000;
  return (transcurridoSeg % duracionVueltaSeg) / duracionVueltaSeg;
}

function puntoEnFraccion(puntos: Punto[], f: number): Punto {
  if (puntos.length === 1) return puntos[0];
  const largos = puntos.slice(1).map((b, i) => distanciaMetros(puntos[i], b));
  const total = largos.reduce((s, x) => s + x, 0);
  if (total <= 0) return puntos[0];

  let restante = Math.min(Math.max(f, 0), 1) * total;
  for (let i = 0; i < largos.length; i++) {
    const largo = largos[i];
    if (restante <= largo && largo > 0) {
      const t = restante / largo;
      const a = puntos[i];
      const b = puntos[i + 1];
      // Mismo orden de operaciones que el backend: así los números coinciden hasta el último decimal útil.
      return { lat: a.lat + (b.lat - a.lat) * t, lng: a.lng + (b.lng - a.lng) * t };
    }
    restante -= largo;
  }
  return puntos[puntos.length - 1];
}

/** Posición del bus a la hora `ahoraMs` (epoch ms), sobre el recorrido principal. */
export function posicionSimulada(puntos: Punto[], inicioMs: number, ahoraMs: number, velocidadKmh: number): Punto {
  if (puntos.length === 0) throw new Error('El recorrido principal no tiene puntos');
  return puntoEnFraccion(puntos, fraccion(longitudMetros(puntos), inicioMs, ahoraMs, velocidadKmh));
}
