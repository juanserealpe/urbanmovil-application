import type { Punto } from '../api/tipos';

export interface LineaMapa {
  id: string;
  puntos: Punto[];
  color: string;
  /** px, por defecto 6. */
  ancho?: number;
  /** Alternos. */
  punteada?: boolean;
  /** 0–1, por defecto 1. */
  opacidad?: number;
}

export type TipoMarcador = 'inicio' | 'final' | 'vertice' | 'bus';

export interface MarcadorMapa {
  id: string;
  punto: Punto;
  tipo: TipoMarcador;
  color?: string;
  /** Texto accesible y tooltip. */
  etiqueta?: string;
  arrastrable?: boolean;
}

export interface MapaBaseProps {
  lineas: LineaMapa[];
  marcadores?: MarcadorMapa[];
  onClickMapa?: (p: Punto) => void;
  /** Se llama mientras se arrastra y al soltar. */
  onMoverMarcador?: (id: string, p: Punto) => void;
  /** Si llega (o cambia su contenido), ajusta la vista a estos puntos. */
  encuadrar?: Punto[];
  alto?: number | string;
  ariaLabel: string;
}
