import 'maplibre-gl/dist/maplibre-gl.css';

import {
  LngLatBounds,
  MapLibreMap,
  Marker,
  NavigationControl,
  type GeoJSONSource,
} from 'maplibre-gl';
import { useEffect, useRef, useState } from 'react';
import type { Punto } from '../api/tipos';
import { CENTRO_INICIAL, ESTILO_MAPA, ZOOM_INICIAL } from './config';
import { aLngLat, aPunto } from './coordenadas';
import clases from './MapaBase.module.css';
import { anclaMarcador, crearElementoMarcador, firmaMarcador } from './marcadores';
import type { LineaMapa, MapaBaseProps, MarcadorMapa } from './tipos';

const FUENTE_LINEAS = 'um-lineas';
const CAPA_SOLIDAS = 'um-lineas-solidas';
const CAPA_PUNTEADAS = 'um-lineas-punteadas';

interface MarcadorVivo {
  marcador: Marker;
  firma: string;
}

/** El tipo que acepta `setData` (sin el caso URL). Así no dependemos de los tipos globales de GeoJSON. */
type DatosGeoJson = Exclude<Parameters<GeoJSONSource['setData']>[0], string>;

function aGeoJson(lineas: LineaMapa[]): DatosGeoJson {
  return {
    type: 'FeatureCollection',
    features: lineas
      // Un LineString necesita al menos dos posiciones.
      .filter((l) => l.puntos.length >= 2)
      .map((l) => ({
        type: 'Feature' as const,
        id: l.id,
        properties: {
          color: l.color,
          ancho: l.ancho ?? 6,
          opacidad: l.opacidad ?? 1,
          punteada: Boolean(l.punteada),
        },
        geometry: { type: 'LineString' as const, coordinates: l.puntos.map(aLngLat) },
      })),
  };
}

function clavePuntos(puntos: Punto[] | undefined): string {
  return puntos ? puntos.map((p) => `${p.lat},${p.lng}`).join(';') : '';
}

/**
 * Mapa base reutilizable (MapLibre + OpenFreeMap). Se crea una sola vez y se destruye al desmontar;
 * las líneas se actualizan con `setData` y los marcadores se sincronizan por `id`.
 */
export function MapaBase({
  lineas,
  marcadores = [],
  onClickMapa,
  onMoverMarcador,
  encuadrar,
  alto = 400,
  ariaLabel,
}: MapaBaseProps) {
  const contenedorRef = useRef<HTMLDivElement>(null);
  const mapaRef = useRef<MapLibreMap | null>(null);
  const marcadoresRef = useRef(new Map<string, MarcadorVivo>());
  const [listo, setListo] = useState(false);

  // Los callbacks se leen desde refs para no re-suscribir eventos del mapa en cada render.
  const onClickRef = useRef(onClickMapa);
  const onMoverRef = useRef(onMoverMarcador);
  useEffect(() => {
    onClickRef.current = onClickMapa;
    onMoverRef.current = onMoverMarcador;
  });

  // ---------- Creación y destrucción ----------
  useEffect(() => {
    const contenedor = contenedorRef.current;
    if (!contenedor) return;

    const mapa = new MapLibreMap({
      container: contenedor,
      style: ESTILO_MAPA,
      center: aLngLat(CENTRO_INICIAL),
      zoom: ZOOM_INICIAL,
    });
    mapaRef.current = mapa;
    mapa.addControl(new NavigationControl({ showCompass: false }), 'top-right');

    mapa.on('load', () => {
      mapa.addSource(FUENTE_LINEAS, { type: 'geojson', data: aGeoJson([]) });
      mapa.addLayer({
        id: CAPA_SOLIDAS,
        type: 'line',
        source: FUENTE_LINEAS,
        filter: ['!', ['get', 'punteada']],
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: {
          'line-color': ['get', 'color'],
          'line-width': ['get', 'ancho'],
          'line-opacity': ['get', 'opacidad'],
        },
      });
      mapa.addLayer({
        id: CAPA_PUNTEADAS,
        type: 'line',
        source: FUENTE_LINEAS,
        filter: ['get', 'punteada'],
        layout: { 'line-join': 'round', 'line-cap': 'butt' },
        paint: {
          'line-color': ['get', 'color'],
          'line-width': ['get', 'ancho'],
          'line-opacity': ['get', 'opacidad'],
          'line-dasharray': [2, 1.5],
        },
      });
      setListo(true);
    });

    mapa.on('click', (e) => {
      // Un clic sobre un marcador también llega al mapa (MapLibre escucha el puntero en el lienzo):
      // se ignora para no agregar un punto encima de un vértice.
      const destino = e.originalEvent.target;
      if (destino instanceof Element && destino.closest('.maplibregl-marker')) return;
      onClickRef.current?.(aPunto(e.lngLat));
    });

    // El mapa solo escucha el resize de la ventana; esto cubre cambios del contenedor (columnas, menú…).
    const observador = new ResizeObserver(() => mapa.resize());
    observador.observe(contenedor);

    const vivos = marcadoresRef.current;
    return () => {
      observador.disconnect();
      vivos.forEach(({ marcador }) => marcador.remove());
      vivos.clear();
      mapa.remove();
      mapaRef.current = null;
      setListo(false);
    };
  }, []);

  // ---------- Modo de edición: cursor y doble clic ----------
  const editable = Boolean(onClickMapa);
  useEffect(() => {
    const mapa = mapaRef.current;
    if (!mapa) return;
    mapa.getCanvas().style.cursor = editable ? 'crosshair' : '';
    // Con doble clic se agregan dos puntos: que no haga además zoom.
    if (editable) mapa.doubleClickZoom.disable();
    else mapa.doubleClickZoom.enable();
  }, [editable, listo]);

  // ---------- Líneas ----------
  useEffect(() => {
    if (!listo) return;
    mapaRef.current?.getSource<GeoJSONSource>(FUENTE_LINEAS)?.setData(aGeoJson(lineas));
  }, [lineas, listo]);

  // ---------- Marcadores ----------
  useEffect(() => {
    const mapa = mapaRef.current;
    if (!mapa) return;
    const vivos = marcadoresRef.current;
    const nuevos = new Map<string, MarcadorMapa>(marcadores.map((m) => [m.id, m]));

    for (const [id, vivo] of vivos) {
      const m = nuevos.get(id);
      if (!m || firmaMarcador(m) !== vivo.firma) {
        vivo.marcador.remove();
        vivos.delete(id);
      }
    }

    for (const m of marcadores) {
      const vivo = vivos.get(m.id);
      if (vivo) {
        vivo.marcador.setLngLat(aLngLat(m.punto));
        continue;
      }
      const marcador = new Marker({
        element: crearElementoMarcador(m),
        anchor: anclaMarcador(m),
        draggable: Boolean(m.arrastrable),
      })
        .setLngLat(aLngLat(m.punto))
        .addTo(mapa);
      if (m.arrastrable) {
        const avisar = () => onMoverRef.current?.(m.id, aPunto(marcador.getLngLat()));
        marcador.on('drag', avisar);
        marcador.on('dragend', avisar);
      }
      vivos.set(m.id, { marcador, firma: firmaMarcador(m) });
    }
  }, [marcadores]);

  // ---------- Encuadre ----------
  const claveEncuadre = clavePuntos(encuadrar);
  const encuadrarRef = useRef(encuadrar);
  useEffect(() => {
    encuadrarRef.current = encuadrar;
  });
  useEffect(() => {
    const mapa = mapaRef.current;
    const puntos = encuadrarRef.current;
    if (!listo || !mapa || !puntos || puntos.length === 0) return;
    const limites = puntos.reduce(
      (acc, p) => acc.extend(aLngLat(p)),
      new LngLatBounds(aLngLat(puntos[0]), aLngLat(puntos[0])),
    );
    mapa.fitBounds(limites, { padding: 48, maxZoom: 16, duration: 0 });
  }, [claveEncuadre, listo]);

  return (
    <div
      ref={contenedorRef}
      className={clases.contenedor}
      style={{ height: alto }}
      role="region"
      aria-label={ariaLabel}
    />
  );
}
