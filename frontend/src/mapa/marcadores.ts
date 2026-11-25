import type { PositionAnchor } from 'maplibre-gl';
import clases from './MapaBase.module.css';
import type { MarcadorMapa } from './tipos';

export const COLOR_INICIO = '#137A3F';
export const COLOR_FINAL = '#12161C';
const COLOR_VERTICE_POR_DEFECTO = '#1F4FD8';
const COLOR_BUS_POR_DEFECTO = '#1F4FD8';

// Ícono «bus» de Tabler, en línea para no montar React dentro de cada marcador.
const SVG_BUS =
  '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" ' +
  'stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
  '<path d="M4 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"/><path d="M16 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"/>' +
  '<path d="M4 17h-2v-11a1 1 0 0 1 1 -1h14a5 7 0 0 1 5 7v5h-2m-4 0h-8"/><path d="M16 5l1.5 7l4.5 0"/>' +
  '<path d="M2 10l15 0"/><path d="M7 5l0 5"/><path d="M12 5l0 5"/></svg>';

/** Firma de lo que obliga a recrear el elemento del marcador (la posición no cuenta). */
export function firmaMarcador(m: MarcadorMapa): string {
  return [m.tipo, m.color ?? '', m.etiqueta ?? '', m.arrastrable ? '1' : '0'].join('|');
}

export function anclaMarcador(m: MarcadorMapa): PositionAnchor {
  return m.tipo === 'inicio' || m.tipo === 'final' ? 'bottom' : 'center';
}

function crearPin(letra: string, color: string): HTMLElement {
  const el = document.createElement('div');
  el.className = clases.pin;
  const cabeza = document.createElement('div');
  cabeza.className = clases.pinCabeza;
  cabeza.style.background = color;
  const texto = document.createElement('span');
  texto.className = clases.pinLetra;
  texto.textContent = letra;
  texto.setAttribute('aria-hidden', 'true');
  cabeza.appendChild(texto);
  el.appendChild(cabeza);
  return el;
}

export function crearElementoMarcador(m: MarcadorMapa): HTMLElement {
  let el: HTMLElement;
  switch (m.tipo) {
    case 'inicio':
      el = crearPin('I', m.color ?? COLOR_INICIO);
      break;
    case 'final':
      el = crearPin('F', m.color ?? COLOR_FINAL);
      break;
    case 'vertice':
      el = document.createElement('div');
      el.className = clases.vertice;
      el.style.borderColor = m.color ?? COLOR_VERTICE_POR_DEFECTO;
      break;
    case 'bus':
      el = document.createElement('div');
      el.className = clases.bus;
      el.style.background = m.color ?? COLOR_BUS_POR_DEFECTO;
      el.innerHTML = SVG_BUS;
      break;
  }

  if (m.arrastrable) el.dataset.arrastrable = '';
  if (m.etiqueta) {
    el.title = m.etiqueta;
    el.setAttribute('aria-label', m.etiqueta);
    el.setAttribute('role', 'img');
  } else {
    el.setAttribute('aria-hidden', 'true');
  }
  return el;
}
