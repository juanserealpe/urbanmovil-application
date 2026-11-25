import type { Ref } from 'react';
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
  /** Contenido del popup que se abre al hacer clic. Se actualiza sin recrear el marcador. */
  popup?: ContenidoPopup;
}

export interface ContenidoPopup {
  titulo: string;
  lineas: string[];
}

/** Acciones imperativas del mapa (p. ej. desde un panel lateral). */
export interface MapaBaseHandle {
  /** Centra el mapa en el marcador y abre su popup, si tiene. */
  enfocarMarcador: (id: string) => void;
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
  ref?: Ref<MapaBaseHandle>;
}
