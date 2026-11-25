import type { CaminoAlterno, Punto } from '../../api/tipos';
import type { LineaMapa, MarcadorMapa } from '../../mapa/tipos';
import { COLOR_ALTERNO } from './constantes';

/** Pines I y F del recorrido principal (F solo si hay al menos dos puntos). */
export function marcadoresInicioFinal(principal: Punto[]): MarcadorMapa[] {
  if (principal.length === 0) return [];
  const inicio: MarcadorMapa = { id: 'inicio', punto: principal[0], tipo: 'inicio', etiqueta: 'Inicio de la ruta' };
  if (principal.length === 1) return [inicio];
  return [
    inicio,
    { id: 'final', punto: principal[principal.length - 1], tipo: 'final', etiqueta: 'Final de la ruta' },
  ];
}

interface RutaDibujable {
  color: string;
  recorridoPrincipal: Punto[];
  alternos: Pick<CaminoAlterno, 'nombre' | 'puntos'>[];
}

/** Vista de solo lectura: principal sólido, alternos punteados en naranja, pines I y F. */
export function dibujarRuta(ruta: RutaDibujable): { lineas: LineaMapa[]; marcadores: MarcadorMapa[] } {
  return {
    lineas: [
      ...ruta.alternos.map((a, i) => ({
        id: `alterno-${i}`,
        puntos: a.puntos,
        color: COLOR_ALTERNO,
        ancho: 5,
        punteada: true,
      })),
      { id: 'principal', puntos: ruta.recorridoPrincipal, color: ruta.color },
    ],
    marcadores: marcadoresInicioFinal(ruta.recorridoPrincipal),
  };
}

/** Todos los puntos de la ruta, para encuadrar el mapa. */
export function puntosDeRuta(ruta: RutaDibujable): Punto[] {
  return [...ruta.recorridoPrincipal, ...ruta.alternos.flatMap((a) => a.puntos)];
}
