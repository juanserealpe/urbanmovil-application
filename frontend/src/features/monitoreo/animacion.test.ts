import { describe, expect, it } from 'vitest';
import type { BusMonitoreo } from '../../api/tipos';
import { posicionesAnimadas, desfaseReloj } from './animacion';

const TRAZADO = [
  { lat: 2.4419, lng: -76.6066 },
  { lat: 2.444, lng: -76.605 },
  { lat: 2.4462, lng: -76.6031 },
  { lat: 2.449, lng: -76.601 },
  { lat: 2.451, lng: -76.599 },
];
const INICIO = '2026-11-24T10:00:00-05:00';
const INICIO_MS = Date.parse(INICIO);

function bus(recorridoId: number, rutaId: number, posicion = { lat: 9, lng: 9 }): BusMonitoreo {
  return {
    recorridoId,
    vehiculo: { id: 1, numeroInterno: 'UM-104', placa: 'ABC104' },
    ruta: { id: rutaId, codigo: 'R1', nombre: 'Ruta R1', color: '#0057B8' },
    conductor: { id: 1, nombre: 'Andrés' },
    posicion,
    horaPosicion: INICIO,
    inicio: INICIO,
  };
}

describe('desfaseReloj', () => {
  it('es la hora del servidor menos la hora local en que llegó la respuesta', () => {
    expect(desfaseReloj('2026-11-24T10:00:05-05:00', INICIO_MS)).toBe(5000);
  });

  it('sin datos no corrige nada', () => {
    expect(desfaseReloj(undefined, INICIO_MS)).toBe(0);
  });
});

describe('posicionesAnimadas', () => {
  it('calcula la posición con el trazado de su ruta a la hora dada (misma que el backend)', () => {
    const posiciones = posicionesAnimadas([bus(7, 1)], new Map([[1, TRAZADO]]), 18, INICIO_MS + 90_000);
    expect(posiciones.get(7)?.lat).toBeCloseTo(2.445065833931734, 9);
    expect(posiciones.get(7)?.lng).toBeCloseTo(-76.60407950705896, 9);
  });

  it('si el trazado todavía no cargó usa la posición que mandó el servidor', () => {
    const delServidor = { lat: 2.44, lng: -76.6 };
    const posiciones = posicionesAnimadas([bus(7, 1, delServidor)], new Map(), 18, INICIO_MS + 90_000);
    expect(posiciones.get(7)).toEqual(delServidor);
  });
});
