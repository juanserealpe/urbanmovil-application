import type { Punto, RutaDetalle } from '../../../api/tipos';
import { MAX_ALTERNOS } from '../constantes';

/** Un camino alterno en edición. `clave` es local y estable; `id` solo existe si vino del backend. */
export interface TrazadoEditable {
  clave: string;
  id?: number;
  nombre: string;
  puntos: Punto[];
}

export const PRINCIPAL = 'principal';
/** Lo que se está trazando: el recorrido principal o la clave de un alterno. */
export type Objetivo = typeof PRINCIPAL | string;

export interface EstadoTrazado {
  principal: Punto[];
  alternos: TrazadoEditable[];
  activo: Objetivo;
  /** true en cuanto el usuario cambia algo (para avisar al cancelar). */
  modificado: boolean;
  /** Para generar claves locales únicas. */
  siguienteClave: number;
}

export type AccionTrazado =
  | { tipo: 'agregarPunto'; punto: Punto }
  | { tipo: 'moverPunto'; indice: number; punto: Punto }
  | { tipo: 'deshacer' }
  | { tipo: 'borrar' }
  | { tipo: 'activar'; objetivo: Objetivo }
  | { tipo: 'agregarAlterno' }
  | { tipo: 'eliminarAlterno'; clave: string }
  | { tipo: 'renombrarAlterno'; clave: string; nombre: string };

export function estadoInicial(ruta: RutaDetalle | null): EstadoTrazado {
  const alternos = (ruta?.alternos ?? []).map((a, i) => ({
    clave: `a${i + 1}`,
    id: a.id,
    nombre: a.nombre,
    puntos: a.puntos,
  }));
  return {
    principal: ruta?.recorridoPrincipal ?? [],
    alternos,
    activo: PRINCIPAL,
    modificado: false,
    siguienteClave: alternos.length + 1,
  };
}

export function puntosDe(estado: EstadoTrazado, objetivo: Objetivo): Punto[] {
  if (objetivo === PRINCIPAL) return estado.principal;
  return estado.alternos.find((a) => a.clave === objetivo)?.puntos ?? [];
}

/** «Alterno N» con el menor N que no esté en uso. */
function nombreLibre(alternos: TrazadoEditable[]): string {
  const usados = new Set(alternos.map((a) => a.nombre.trim()));
  let n = alternos.length + 1;
  for (let i = 1; i <= alternos.length + 1; i++) {
    if (!usados.has(`Alterno ${i}`)) {
      n = i;
      break;
    }
  }
  return `Alterno ${n}`;
}

function editarPuntosActivos(estado: EstadoTrazado, cambiar: (puntos: Punto[]) => Punto[]): EstadoTrazado {
  if (estado.activo === PRINCIPAL) {
    return { ...estado, principal: cambiar(estado.principal), modificado: true };
  }
  return {
    ...estado,
    alternos: estado.alternos.map((a) => (a.clave === estado.activo ? { ...a, puntos: cambiar(a.puntos) } : a)),
    modificado: true,
  };
}

export function reducirTrazado(estado: EstadoTrazado, accion: AccionTrazado): EstadoTrazado {
  switch (accion.tipo) {
    case 'agregarPunto':
      // El orden de los clics es el sentido de circulación: siempre al final.
      return editarPuntosActivos(estado, (puntos) => [...puntos, accion.punto]);

    case 'moverPunto':
      return editarPuntosActivos(estado, (puntos) =>
        accion.indice < 0 || accion.indice >= puntos.length
          ? puntos
          : puntos.map((p, i) => (i === accion.indice ? accion.punto : p)),
      );

    case 'deshacer':
      if (puntosDe(estado, estado.activo).length === 0) return estado;
      return editarPuntosActivos(estado, (puntos) => puntos.slice(0, -1));

    case 'borrar':
      if (puntosDe(estado, estado.activo).length === 0) return estado;
      return editarPuntosActivos(estado, () => []);

    case 'activar': {
      const existe = accion.objetivo === PRINCIPAL || estado.alternos.some((a) => a.clave === accion.objetivo);
      return existe ? { ...estado, activo: accion.objetivo } : estado;
    }

    case 'agregarAlterno': {
      if (estado.alternos.length >= MAX_ALTERNOS) return estado;
      const clave = `a${estado.siguienteClave}`;
      return {
        ...estado,
        alternos: [...estado.alternos, { clave, nombre: nombreLibre(estado.alternos), puntos: [] }],
        activo: clave,
        siguienteClave: estado.siguienteClave + 1,
        modificado: true,
      };
    }

    case 'eliminarAlterno':
      // En el PUT, un alterno que no se envía se elimina en el backend.
      return {
        ...estado,
        alternos: estado.alternos.filter((a) => a.clave !== accion.clave),
        activo: estado.activo === accion.clave ? PRINCIPAL : estado.activo,
        modificado: true,
      };

    case 'renombrarAlterno':
      return {
        ...estado,
        alternos: estado.alternos.map((a) => (a.clave === accion.clave ? { ...a, nombre: accion.nombre } : a)),
        modificado: true,
      };
  }
}
