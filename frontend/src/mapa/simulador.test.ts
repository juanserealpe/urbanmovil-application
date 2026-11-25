import { describe, expect, it } from 'vitest';
import { longitudMetros, posicionSimulada } from './simulador';

/**
 * Mismo trazado y mismos valores esperados que `SimuladorPosicionTest` (backend) y
 * `SimuladorRecorridoTest` (app). Si alguno cambia, web y app dejan de ver el bus en el mismo lugar.
 */
const TRAZADO = [
  { lat: 2.4419, lng: -76.6066 },
  { lat: 2.444, lng: -76.605 },
  { lat: 2.4462, lng: -76.6031 },
  { lat: 2.449, lng: -76.601 },
  { lat: 2.451, lng: -76.599 },
];
const INICIO = Date.parse('2026-11-24T10:00:00-05:00');
const seg = (s: number) => INICIO + s * 1000;

describe('simulador de posición (contrato compartido con backend y app)', () => {
  it('mide el recorrido con haversine', () => {
    expect(longitudMetros(TRAZADO)).toBeCloseTo(1319.9881103444893, 6);
  });

  it('recién iniciado está en el primer punto', () => {
    expect(posicionSimulada(TRAZADO, INICIO, INICIO, 18)).toEqual(TRAZADO[0]);
  });

  it('a los 90 s coincide con el backend', () => {
    const p = posicionSimulada(TRAZADO, INICIO, seg(90), 18);
    expect(p.lat).toBeCloseTo(2.445065833931734, 9);
    expect(p.lng).toBeCloseTo(-76.60407950705896, 9);
  });

  it('a los 600 s ya dio la vuelta y vuelve a empezar', () => {
    const p = posicionSimulada(TRAZADO, INICIO, seg(600), 18);
    expect(p.lat).toBeCloseTo(2.444453192996366, 9);
    expect(p.lng).toBeCloseTo(-76.6046086060486, 9);
  });

  it('antes del inicio (reloj atrasado) se queda en el primer punto', () => {
    expect(posicionSimulada(TRAZADO, INICIO, seg(-30), 18)).toEqual(TRAZADO[0]);
  });

  it('un trazado de un solo punto no se mueve', () => {
    expect(posicionSimulada([TRAZADO[2]], INICIO, seg(50), 18)).toEqual(TRAZADO[2]);
  });
});
