# UrbanMovil · Frontend administrativo · Fase 2 de 3 — Rutas y editor con mapa

> Documento de implementación para el agente de desarrollo. **Requiere la Fase 1 terminada** (proyecto, tema, cliente de API, sesión, menú, manejo de errores). Implementar solo esta fase. El contrato de la API está en **`FRONTEND-API.md`** (sección 5.4). Si algo de pantalla o de negocio no está claro, **detente y pregunta**.

## 1. Contexto breve

Panel web del administrador de UrbanMovil (React + TypeScript + Vite + Mantine + React Query + react-hook-form/zod). Siguen vigentes todas las reglas de la Fase 1: librerías permitidas, estructura de carpetas, `utils/errores.ts` en todos los formularios, textos en español, accesibilidad mínima.

Mockups de referencia: pantallas **02 (Rutas)** y **03 (Crear / editar ruta)**.

## 2. Objetivo de esta fase

1. Componente de mapa reutilizable (`src/mapa/`), que también usará la Fase 3.
2. Lista de rutas con vista previa y eliminación (HU-04, HU-05).
3. Editor de ruta para crear y modificar, con trazado del recorrido principal y de los caminos alternos en el mapa (HU-02, HU-03, HU-04).
4. `/` pasa a redirigir a `/rutas`.

## 3. Mapa base (`src/mapa/`)

### 3.1 Decisión

Se usa **`maplibre-gl` directamente** con el estilo gratuito de OpenFreeMap (sin API key), el mismo de la app móvil del pasajero. `FRONTEND-API.md` sugiere Leaflet, pero **no lo uses**: el equipo ya decidió MapLibre. `@geoman-io/maplibre-geoman-free` no se usa; desinstálalo (`npm uninstall @geoman-io/maplibre-geoman-free`).

- Estilo: `https://tiles.openfreemap.org/styles/liberty`
- Centro inicial: Popayán, `lng -76.606, lat 2.444`, zoom 14.
- Importar `maplibre-gl/dist/maplibre-gl.css`.
- Mostrar la atribución de OpenStreetMap que trae el estilo y el control de zoom (`NavigationControl`).

**Ojo con el orden de coordenadas:** la API usa `{ lat, lng }`, pero MapLibre y GeoJSON usan `[lng, lat]`. Crea en `src/mapa/coordenadas.ts` las únicas dos funciones de conversión (`aLngLat(p: Punto)` y `aPunto(lngLat)`) y úsalas siempre.

### 3.2 Componente

```ts
interface LineaMapa {
  id: string;
  puntos: Punto[];
  color: string;
  ancho?: number;        // px, por defecto 6
  punteada?: boolean;    // alternos
  opacidad?: number;
}
interface MarcadorMapa {
  id: string;
  punto: Punto;
  tipo: 'inicio' | 'final' | 'vertice' | 'bus';
  color?: string;
  etiqueta?: string;     // texto accesible y tooltip
  arrastrable?: boolean;
}
interface MapaBaseProps {
  lineas: LineaMapa[];
  marcadores?: MarcadorMapa[];
  onClickMapa?: (p: Punto) => void;
  onMoverMarcador?: (id: string, p: Punto) => void;
  encuadrar?: Punto[];   // si llega, ajusta la vista a estos puntos (fitBounds con margen)
  alto?: number | string;
  ariaLabel: string;
}
```

- Crea el mapa una sola vez (`useRef` + `useEffect`) y destrúyelo al desmontar (`map.remove()`).
- Las líneas van en **una fuente GeoJSON** con una capa para las sólidas y otra para las punteadas (`line-dasharray`); al cambiar `lineas`, actualiza con `setData` en vez de recrear capas.
- Los marcadores son `maplibregl.Marker` con un elemento HTML propio:
  - `inicio`: pin verde `#137A3F` con letra «I».
  - `final`: pin oscuro `#12161C` con letra «F».
  - `vertice`: círculo blanco de 12 px con borde del color de la línea.
  - `bus`: círculo de 30 px del color de la ruta con ícono de bus blanco (Fase 3).
- El contenedor tiene `role="region"` y `aria-label`.

## 4. Lista de rutas (`/rutas`) — mockup 02

- Encabezado: «Rutas», subtítulo «Rutas de la empresa que ven pasajeros y conductores», botón primario **Nueva ruta** (→ `/rutas/nueva`).
- Buscador «Buscar por código o nombre» (espera de 300 ms, parámetro `q`) y contador «N rutas».
- Tabla con `RutaResumen`:

| Columna | Contenido |
| --- | --- |
| Ruta | Insignia cuadrada con el `codigo` sobre el `color` de la ruta + `nombre` en negrita + «Editada [fecha]» (`actualizadoEn`) |
| Recorrido principal | «N puntos» (`puntosRecorrido`) |
| Alternos | `numAlternos` |
| Asignada hoy a | `conductoresHoy` separados por coma, o «Sin asignar» |
| Estado | Chip azul con ícono de bus «N recorridos activos» si `recorridosActivos > 0`; si no, chip gris «Sin recorridos activos» |
| Acciones | **Editar** (→ `/rutas/:id`) y **Eliminar** |

- Clic en una fila (fuera de los botones) la selecciona y muestra debajo la **vista previa**: `MapaBase` de 300 px de alto con el recorrido principal sólido, los alternos punteados en naranja `#C25E00`, pines I y F, y una leyenda («Recorrido principal» con muestra sólida, «Camino alterno» con muestra punteada). La vista previa carga `GET /rutas/{id}` (query `['ruta', id]`). Por defecto se selecciona la primera ruta.
- Eliminar: confirmación «¿Eliminar la ruta [código]?» con el texto «Dejará de verse para pasajeros y conductores y se retirará de las programaciones. No se puede deshacer.» Botón rojo **Eliminar ruta**. Si `recorridosActivos > 0`, el botón de la tabla está deshabilitado con tooltip «Tiene recorridos activos».
- Estado vacío: «Todavía no hay rutas. Cree la primera con Nueva ruta.»

## 5. Editor de ruta (`/rutas/nueva` y `/rutas/:id`) — mockup 03

### 5.1 Disposición

Dos columnas (en pantallas angostas, el panel va debajo del mapa):

- **Izquierda (flexible, 680 px de alto):** `MapaBase` con una barra de herramientas flotante arriba y una ayuda flotante abajo.
- **Derecha (~360 px):** tarjetas de datos, recorrido principal y caminos alternos, más los botones.

Título: «Nueva ruta» o «Editar ruta [código]», subtítulo «Trace el recorrido principal y los caminos alternos». Al editar, el formulario se llena con `GET /rutas/{id}`.

### 5.2 Herramienta de trazado

Estado local del editor:

```ts
interface TrazadoEditable { clave: string; id?: number; nombre: string; puntos: Punto[] }
// principal: Punto[]; alternos: TrazadoEditable[]; activo: 'principal' | clave de un alterno
```

- **Selector de lo que se está trazando** (control segmentado arriba del mapa): «Recorrido principal», «Alterno 1», «Alterno 2»… Solo se edita el trazado activo.
- **Clic en el mapa** → agrega un punto **al final** del trazado activo. El orden de los clics es el sentido de circulación.
- **Arrastrar un vértice** del trazado activo → lo mueve.
- **Deshacer punto** → quita el último punto del trazado activo.
- **Borrar trazado** → vacía el trazado activo (con confirmación si tiene más de 2 puntos).
- Trazado activo: línea sólida del color de la ruta (principal) o naranja (alterno) con vértices visibles y arrastrables. Trazados inactivos: sin vértices, más tenues (opacidad 0.5); alternos siempre punteados.
- Pines **I** (primer punto) y **F** (último punto) del recorrido principal siempre visibles.
- Ayuda flotante abajo a la izquierda (fondo oscuro, texto blanco): «Haga clic en el mapa para agregar puntos en el sentido de circulación. El primer punto es el inicio y el último el final. Arrastre un punto para moverlo.»
- Al editar una ruta existente, el mapa se encuadra al recorrido principal al cargar.

### 5.3 Panel derecho

1. **Datos**: **Nombre de la ruta** (obligatorio, máx. 120; ayuda «Debe ser único»), **Código** (obligatorio, máx. 10, p. ej. R1; se muestra en mayúsculas) y **Color** (`ColorInput` de Mantine con formato hex y las 6 muestras de la paleta del backend: `#0057B8`, `#D62828`, `#2A9D8F`, `#F4A261`, `#6A4C93`, `#E76F51`; opcional al crear, con ayuda «Si lo deja vacío, se asigna uno automáticamente»).
2. **Recorrido principal**: chips de estado calculados en vivo: «N puntos», «Inicio marcado» y «Final marcado» (verdes cuando hay 2 o más puntos; naranjas «Falta trazar» si no). Texto: «Se usa para tiempos de llegada y búsqueda. Conserva el sentido en que se trazó.»
3. **Caminos alternos**: lista con muestra punteada naranja, **nombre editable** en línea (obligatorio, máx. 120), «N puntos», botones **Trazar** (lo activa en el mapa) y **Eliminar**. Botón **Agregar camino alterno** (crea uno vacío llamado «Alterno N» y lo activa; deshabilitado al llegar a 10). Texto: «Solo informativos para el pasajero.»
4. Botones **Cancelar** (vuelve a `/rutas`; si hay cambios sin guardar, pide confirmación) y **Guardar ruta**.

### 5.4 Guardar

- Validación en el cliente antes de enviar (zod), con los mismos mensajes del backend: «Escriba el nombre de la ruta», «Escriba el código de la ruta», «Falta trazar el recorrido principal», «La ruta debe tener punto de inicio y punto final», «Escriba el nombre del camino alterno», «Cada camino alterno debe tener al menos dos puntos».
- Cuerpo: `RutaRequest`. En el `PUT` se envía **siempre la lista completa** de alternos: los que vinieron del backend con su `id` y los nuevos sin `id`. Un alterno eliminado en la UI simplemente no se envía.
- No envíes `color` si el campo está vacío.
- Errores del backend con `utils/errores.ts`. Para trazados, los campos llegan como `recorridoPrincipal`, `alternos`, `alternos[i].puntos`, `alternos[i].nombre`, `alternos[i].id`: muéstralos en la tarjeta correspondiente (recorrido principal o el alterno de índice `i`) y además en una alerta general arriba de los botones.
- Éxito: notificación «Ruta creada» / «Cambios guardados», invalidar `['rutas']` y `['ruta', id]`, y volver a `/rutas` con esa ruta seleccionada.

### 5.5 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-02.1 | Nombre nuevo y recorrido trazado | Guarda | La ruta aparece en la lista y en la vista previa |
| CA-02.2 | Sin recorrido principal | Guarda | «Falta trazar el recorrido principal», sin llamar a la API |
| CA-02.3 | Nombre de otra ruta | Guarda | «Ya existe una ruta con este nombre» bajo el nombre |
| CA-02.4 | Recorrido con un solo punto | Guarda | «La ruta debe tener punto de inicio y punto final» |
| CA-02.5 | Recorrido trazado | Mira el mapa | Pines I y F en el primer y último punto, en el orden de los clics |
| CA-03.1 | Ruta con recorrido | Agrega un alterno, lo traza y guarda | El alterno se ve punteado, distinto del principal |
| CA-03.2 | Ruta con 2 alternos | Abre la vista previa | Se ven los dos, con la leyenda |
| CA-04.1 | Ruta existente | Cambia nombre o mueve puntos y guarda | La lista y la vista previa muestran la versión nueva |
| CA-04.2 | Ruta en edición | Borra el recorrido principal y guarda | «Falta trazar el recorrido principal»; no se guarda |
| CA-04.3 | Ruta con 2 alternos | Elimina uno y guarda | Al volver a abrirla, solo queda uno |
| CA-05.1 | Ruta sin recorridos activos | Elimina y confirma | Desaparece de la lista |
| CA-05.2 | Ruta con recorridos activos | Mira la tabla | Eliminar deshabilitado; un 409 se muestra si ocurre |
| CA-05.3 | Diálogo de confirmación | Cancela | No se elimina |

## 6. Definición de terminado (Fase 2)

- [ ] `MapaBase` reutilizable, sin fugas (se destruye al desmontar) y con la conversión de coordenadas centralizada.
- [ ] Lista de rutas con vista previa, leyenda y eliminación.
- [ ] Editor de crear y editar con trazado por clics, arrastre de vértices, deshacer, borrar, alternos y color.
- [ ] Todos los CA de §5.5 probados a mano contra el backend en perfil `demo`.
- [ ] `/` redirige a `/rutas`; el menú marca «Rutas».
- [ ] `npm run build` y `npm run lint` sin errores.
- [ ] README actualizado (pantallas nuevas y lista de verificación de esta fase).

## 7. Trazabilidad

| Pantalla | HU | Mockup |
| --- | --- | --- |
| Lista de rutas y vista previa | HU-04, HU-05 | 02 |
| Editor de ruta | HU-02, HU-03, HU-04 | 03 |
