# UrbanMovil · MVP Pasajero · Fase 2 de 3 — Mapa, rutas y buses en vivo

> Documento de implementación para el agente de desarrollo. **Requiere la Fase 1 terminada** (proyecto, `TransitRepository`, `BusSimulator` por hora, `domain/` y login). Implementar solo esta fase.

## 1. Contexto breve

App para pasajeros de buses urbanos de Popayán, hecha con **Kotlin Multiplatform + Compose Multiplatform** (solo target Android): la UI y la lógica viven en `shared/src/commonMain`, y `androidApp` solo tiene el `MainActivity`. Prototipo **sin backend**: rutas y buses quemados; los buses se mueven según la hora del teléfono, así todos los teléfonos los ven en la misma posición. Las reglas de stack y prohibiciones de la Fase 1 siguen vigentes (sin HTTP propio, sin base de datos, sin Firebase/Supabase, sin Hilt, sin `android.*` ni `java.*` en `commonMain`).

## 2. Objetivo de esta fase

1. Un componente de mapa reutilizable (`MapaUrbano`) que dibuje rutas, alternos, buses y pines.
2. Pestaña **Rutas** (HU-18): mapa + lista de rutas.
3. Pantalla **Detalle de ruta** (HU-18, HU-19, HU-24): recorrido principal y alternos con leyenda, buses en vivo con su vigencia y ocupación.

## 3. Librería de mapa

**Decisión asumida (cambiable por el equipo):** MapLibre con el estilo gratuito de **OpenFreeMap**, que no requiere API key, registro ni cuenta de facturación.

- Estilo: `https://tiles.openfreemap.org/styles/liberty`
- Librería: **MapLibre Compose**, que es Compose Multiplatform y por eso encaja con el proyecto.
  - `org.maplibre.compose:maplibre-compose` como dependencia de `commonMain` en `shared`.
  - Un runtime Android (p. ej. `org.maplibre.compose:maplibre-compose-runtime-opengl-android`) como `runtimeOnly` del lado Android, con la **misma versión** que la librería.
  - Consulta la documentación vigente en https://maplibre.org/maplibre-compose/ (guías «Getting started», «Styling», «Control the camera» y «Add data to the map»), porque su API cambia entre versiones.
- Plan B si MapLibre Compose bloquea el avance: declarar `MapaUrbano` como `expect` en `commonMain` e implementarlo como `actual` en `shared/src/androidMain` con el **SDK nativo de MapLibre para Android** (`org.maplibre.gl:android-sdk`) dentro de un `AndroidView`. Las pantallas no cambian.
- **No usar** Google Maps Compose (solo funciona en Android y pide API key) ni osmdroid (proyecto archivado desde noviembre de 2024).
- Mostrar la atribución de OpenStreetMap que exige el estilo.
- El permiso `INTERNET` va en `androidApp/src/main/AndroidManifest.xml`.

Encapsular todo en un único composable de `shared/src/commonMain/.../ui/map/` para poder cambiar de librería sin tocar las pantallas:

```kotlin
@Composable
fun MapaUrbano(
    centro: Punto,
    zoom: Double,
    lineas: List<LineaMapa>,       // polilíneas: color, ancho, sólida o punteada
    marcadores: List<MarcadorMapa>,// buses, pines A/B/D/P, mi ubicación
    onTocarMapa: ((Punto) -> Unit)? = null, // se usa en la Fase 3
    modifier: Modifier = Modifier
)
```

Si el teléfono no tiene internet, las teselas no cargan pero las líneas y marcadores deben seguir dibujándose sobre fondo liso. No es necesario manejar mapas sin conexión.

## 4. Pestaña Rutas — HU-18

**HU-18** — Como pasajero, quiero ver la lista de rutas y, al elegir una, su recorrido principal y sus caminos alternos en colores distintos, para saber por dónde pasa y no confiarme solo del camino principal.

Pantalla según mockup «02 · Rutas»:

- Barra superior «Rutas» con subtítulo «3 rutas · datos de demostración».
- Mapa en la mitad superior, centrado en Popayán, con todas las rutas: la seleccionada (por defecto R1) resaltada con su color; las demás en gris semitransparente. Marcadores de los buses visibles y de «Mi ubicación».
- Botón flotante «Centrar en mi ubicación» (vuelve al punto fijo de la Fase 1).
- Hoja inferior con la lista de rutas: insignia con el código (R1, R2, R3) en el color de la ruta, nombre y número de buses activos («2 buses activos», «1 bus activo», «Sin buses activos»). Contar solo buses no OCULTOS.
- Tocar una ruta abre `detalleRuta/{rutaId}`.

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-18.1 | Rutas registradas | El pasajero abre la pestaña Rutas | Ve las 3 rutas en la lista y en el mapa |
| CA-18.2 | Una ruta con alternos (R1) | La elige | Ve el recorrido principal y los alternos en colores distintos, con una leyenda que los identifica |
| CA-18.3 | Ninguna ruta (repositorio vacío) | Abre la lista | Ve «No hay rutas disponibles». Probar cambiando temporalmente `FakeData` o con un repositorio de prueba |

## 5. Detalle de ruta — HU-18, HU-19, HU-24

Pantalla según mockup «03 · Detalle de ruta y buses»:

- Barra superior con flecha atrás, «R1 · Centro – Campanario» y subtítulo «Recorrido principal + N alternos».
- Mapa encuadrado a la ruta:
  - Recorrido principal: línea sólida, color de la ruta, 6–7 dp.
  - Alternos: línea **punteada** naranja `#C25E00`, 5 dp.
  - Pin verde «I» en el punto de inicio y pin oscuro «F» en el punto final.
  - Buses de esa ruta.
- Leyenda bajo el mapa: «Recorrido principal» (muestra sólida) y «Camino alterno» (muestra punteada). Se distingue por **forma**, no solo por color.
- Lista «Buses en recorrido», un elemento por bus no OCULTO:
  - Código del bus (p. ej. «Bus UM-104»).
  - Chip de vigencia: «Ubicación reciente» o «Desactualizada».
  - «Última ubicación: 7:42:18 p. m. (hace 12 s)», en **hora de Colombia** (`America/Bogota`), formato de 12 horas. Convertir con `kotlinx-datetime` (`Instant.toLocalDateTime(TimeZone.of("America/Bogota"))`) y armar el texto a mano o con su API de formato; no usar `java.time` ni `SimpleDateFormat`.
  - Chip de ocupación (si `MOSTRAR_OCUPACION`).
- La lista y los marcadores se actualizan cada segundo desde el `Flow` del repositorio.

### 5.1 Marcadores de bus

| Vigencia | Aspecto |
| --- | --- |
| RECIENTE | Círculo relleno con el color de la ruta, borde blanco, ícono de bus blanco |
| DESACTUALIZADA | Círculo blanco con **borde gris punteado**, ícono gris y etiqueta oscura «Desactualizado» al lado |
| OCULTA | No se dibuja |

Al tocar un marcador de bus: tarjeta o hoja pequeña con código, ruta, hora de última ubicación y ocupación.

### 5.2 HU-19 — Buses activos y vigencia

**HU-19** — Como pasajero, quiero ver en el mapa los buses en recorrido de las rutas que consulto, con la hora de su última ubicación, para saber dónde van y si ese dato es reciente o está desactualizado.

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-19.1 | Un bus con ubicación de 60 s o menos | El pasajero ve el mapa | El bus aparece como reciente con la hora de su última ubicación en hora de Colombia |
| CA-19.2 | Un bus con ubicación de más de 60 s y hasta 15 min | El pasajero ve el mapa | El bus aparece con aspecto distinto y la etiqueta «Desactualizado», no solo con color |
| CA-19.3 | Un bus con ubicación de más de 15 min | El pasajero ve el mapa | El bus no aparece |
| CA-19.4 | Una ruta sin buses no ocultos | El pasajero la consulta | Se muestra «No hay buses activos en esta ruta» |

Para la demo: `UM-221` (R1) pasa a «Desactualizado» entre los minutos 10 y 13 de cada vuelta; el bus de R3 desaparece durante su ventana larga. Añade en modo debug un control oculto (p. ej. mantener presionado el título) que adelante el reloj simulado varios minutos usando `ajusteRelojMs` del repositorio (Fase 1, §6), para poder enseñar estos estados sin esperar. Solo se activa cuando `App(esDebug = true)`, valor que `MainActivity` toma de `BuildConfig.DEBUG`; en un build release no aparece.

### 5.3 HU-24 — Ocupación (solo si `MOSTRAR_OCUPACION = true`)

**HU-24** — Como pasajero, quiero ver si el bus viene con puestos, solo de pie o lleno, y cuándo se reportó, para decidir si lo tomo.

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-24.1 | Un reporte de 10 min o menos | El pasajero ve el bus | Se muestra el nivel («Con puestos», «Solo de pie», «Lleno»), «hace N min» y el aviso «Dato aproximado, no 100 % confiable» |
| CA-24.2 | El último reporte tiene más de 10 min | El pasajero ve el bus | Se muestra «Sin información de ocupación» |
| CA-24.3 | Un bus sin reportes | El pasajero lo ve | Se muestra «Sin información de ocupación» |

Con la bandera en `false` no debe verse nada relacionado con ocupación.

## 6. Accesibilidad mínima

- `contentDescription` (o `Modifier.semantics`) en el mapa («Mapa de la ruta R1»), en cada marcador de bus («Bus UM-104, ubicación reciente») y en botones con ícono.
- Estados (reciente/desactualizado, principal/alterno) distinguibles sin depender del color.
- Contraste de texto 4.5:1 como mínimo.

## 7. Definición de terminado (Fase 2)

- [ ] `MapaUrbano` encapsula la librería de mapa; ninguna pantalla importa clases de MapLibre directamente.
- [ ] Todo el código nuevo vive en `shared/src/commonMain` (salvo un posible `actual` del plan B en `androidMain`); `commonMain` sigue sin importar `android.*` ni `java.*`.
- [ ] Pestaña Rutas cumple CA-18.1 a CA-18.3.
- [ ] Detalle de ruta cumple CA-18.2 y CA-19.1 a CA-19.4.
- [ ] Ocupación cumple CA-24.1 a CA-24.3 y se apaga con la bandera.
- [ ] Los buses se mueven solos y dos teléfonos abiertos a la vez muestran los buses en la misma posición (± 1–2 s).
- [ ] Reabrir la app no reinicia la posición de los buses.
- [ ] Control debug para adelantar el reloj disponible solo en build debug.
- [ ] Pruebas de la Fase 1 siguen pasando.

## 8. Fuera del alcance de esta fase

«Cuándo pasa», búsqueda origen–destino y guía (Fase 3). Toque en el mapa para elegir puntos (Fase 3; `onTocarMapa` ya queda previsto).

## 9. Trazabilidad

| Pantalla / elemento | HU | Reglas |
| --- | --- | --- |
| Pestaña Rutas | HU-18 | RN-01, RN-02 |
| Detalle de ruta, leyenda, alternos | HU-18 | RN-02, RN-03 |
| Marcadores y lista de buses | HU-19 | RN-07, RN-16, RNF-03 |
| Chip de ocupación | HU-24 | RN-14, D-01 (pendiente) |
