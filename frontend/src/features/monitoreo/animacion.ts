import type { BusMonitoreo, Punto } from '../../api/tipos';
import { posicionSimulada } from '../../mapa/simulador';

/**
 * Cuánto va adelantado el reloj del servidor respecto al del navegador, en ms.
 * Se ignora la latencia de la red (decenas de ms = centímetros a 18 km/h).
 * @param recibidoEnMs hora LOCAL en que llegó la respuesta (`dataUpdatedAt` de React Query).
 */
export function desfaseReloj(generadoEn: string | undefined, recibidoEnMs: number): number {
  return generadoEn ? Date.parse(generadoEn) - recibidoEnMs : 0;
}

/**
 * Posición de cada bus (por `recorridoId`) a la hora del servidor `ahoraServidorMs`.
 * Mientras el trazado de su ruta no haya llegado, se usa la última posición del servidor.
 */
export function posicionesAnimadas(
  buses: BusMonitoreo[],
  trazadosPorRuta: Map<number, Punto[]>,
  velocidadKmh: number,
  ahoraServidorMs: number,
): Map<number, Punto> {
  return new Map(
    buses.map((b) => {
      const trazado = trazadosPorRuta.get(b.ruta.id);
      const punto =
        trazado && trazado.length > 0
          ? posicionSimulada(trazado, Date.parse(b.inicio), ahoraServidorMs, velocidadKmh)
          : b.posicion;
      return [b.recorridoId, punto];
    }),
  );
}
