# UrbanMovil · Frontend administrativo · Fase 3 de 3 — Programación, reasignaciones, monitoreo y recorridos de demostración

> Documento de implementación para el agente de desarrollo. **Requiere las fases 1 y 2 terminadas** (en especial `MapaBase` de `src/mapa/`). Implementar solo esta fase. El contrato de la API está en **`FRONTEND-API.md`** (secciones 5.5 a 5.8 y 6). Si algo de pantalla o de negocio no está claro, **detente y pregunta**.

## 1. Contexto breve

Panel web del administrador de UrbanMovil (React + TypeScript + Vite + Mantine + React Query + react-hook-form/zod + maplibre-gl). Siguen vigentes las reglas de las fases 1 y 2.

La app del conductor **no existe**. Los recorridos activos los crea el backend con datos semilla o desde la pantalla de **Recorridos de demostración** (§6), y el monitoreo muestra posiciones **simuladas**.

Mockups de referencia: **07 (Programación del día)**, **08 (Reasignar por eventualidad)** y **09 (Monitoreo)**. La pantalla de recorridos de demostración no tiene mockup.

## 2. Objetivo de esta fase

1. Programación del día con detalle por conductor (HU-09).
2. Reasignación por eventualidad e historial (HU-25).
3. Monitoreo con mapa y polling (HU-31).
4. Recorridos de demostración, solo cuando el backend corre con el perfil `demo`.

Nuevas query keys y sus invalidaciones:

| Query key | Se invalida después de |
| --- | --- |
| `['programaciones', fecha]` | guardar programación, reasignar, iniciar o finalizar recorrido demo, cambiar disponibilidad |
| `['programacion', conductorId, fecha]` | guardar programación, reasignar, iniciar o finalizar recorrido demo |
| `['reasignaciones', filtros]` | reasignar |
| `['monitoreo']` | iniciar o finalizar recorrido demo (además del polling) |
| `['demo', 'recorridos', estado]` | iniciar o finalizar recorrido demo |

Al iniciar o finalizar un recorrido, invalida también `['conductores']`, `['vehiculos']` y `['rutas']`, porque cambian `enRecorrido` y `recorridosActivos`.

## 3. Programación del día (`/programacion`) — mockup 07

**HU-09** — Como administrador de la empresa, quiero asignar a un conductor, antes de su primer recorrido, las rutas que hará en el día y el orden en que las hará, para que el conductor sepa qué rutas tiene y el sistema pueda pasarlo de una a la siguiente.

### 3.1 Disposición

- Encabezado: «Programación del día», subtítulo «Rutas que cada conductor hará y en qué orden», y a la derecha un `DatePickerInput` **Fecha** (por defecto `hoyBogota()`). La fecha va en la URL (`/programacion?fecha=2026-11-24&conductor=3`) para poder recargar sin perderla.
- **Columna izquierda (~300 px): conductores** con `GET /programaciones?fecha=`. Cada tarjeta clicable muestra nombre, «N rutas programadas» y, si aplica, chip «En recorrido · R1» o chip naranja «No disponible». La seleccionada tiene borde azul de 2 px. Por defecto, la primera.
- **Columna derecha: detalle del conductor seleccionado** con `GET /conductores/{id}/programacion?fecha=`.

### 3.2 Detalle

- Tarjeta con nombre, «Programación del [fecha larga] · se hace en este orden» y chips de disponibilidad y de recorrido.
- **Lista ordenada** de `RutaProgramada`: número de orden, insignia con código y color, nombre y estado:
  - `PENDIENTE` → texto gris «Pendiente».
  - `EN_RECORRIDO` → chip azul «En recorrido», ícono de candado y texto «No se puede retirar hasta que termine». Sin botón de retirar.
  - `RETIRO_PENDIENTE` → chip naranja «Se retira al terminar el recorrido». Sin botones.
- Reordenar con botones **Subir** y **Bajar** por fila (con `aria-label`). **No** se usa arrastrar y soltar.
- **Retirar** por fila (salvo los dos estados bloqueados).
- **Agregar ruta**: `Select` con las rutas de `GET /rutas` que **no** están en la lista, y botón **Agregar** (la pone al final). Texto: «Las rutas ya asignadas no aparecen en la lista.»
- Los cambios son locales hasta tocar **Guardar programación**, que envía `PUT /conductores/{id}/programacion?fecha=` con los ids **en el orden visible**. Si hay cambios sin guardar, se muestra una franja «Tiene cambios sin guardar» y botón **Descartar**. Al cambiar de conductor o de fecha con cambios pendientes, pedir confirmación.
- Botón **Reasignar por eventualidad** (abre §4), visible solo cuando la fecha es hoy.
- **Fecha pasada** (`fecha < hoyBogota()`): todo en solo lectura (sin agregar, retirar, mover ni guardar) y aviso «Las programaciones de días pasados no se pueden modificar.»
- Debajo: **Historial de reasignaciones** del conductor en esa fecha (§4.3).

### 3.3 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-09.1 | Conductor sin rutas y 2 rutas registradas | Agrega R2 y R1 y guarda | Se ven en ese orden tras recargar |
| CA-09.2 | Ruta pendiente | La retira y guarda | Ya no aparece |
| CA-09.3 | Ruta en recorrido | Mira la fila | Sin botón de retirar, con candado; un 409 se muestra si ocurre |
| CA-09.4 | Ruta ya asignada | Abre «Agregar ruta» | No aparece en las opciones |
| CA-09.5 | Conductor con varias rutas | Cambia el orden y guarda | Queda el nuevo orden |
| — | Fecha pasada | Abre el detalle | Solo lectura con el aviso |

## 4. Reasignación por eventualidad — mockup 08

**HU-25** — Como administrador de la empresa, quiero reasignar las rutas de un conductor durante el día, escribiendo qué pasó e indicando qué rutas se le asignan y cuáles se le quitan, para cubrir una ruta cuando ocurre una eventualidad y dejar constancia del motivo.

### 4.1 Modal «Reasignar rutas de [nombre]»

- **¿Qué pasó? (obligatorio)**: textarea, máx. 300.
- Dos grupos de casillas lado a lado (uno debajo del otro en pantallas angostas):
  - **Rutas que se le asignan**: solo las rutas que **no** tiene hoy. Texto: «Se agregan al final de su programación.»
  - **Rutas que se le quitan**: solo las que **sí** tiene hoy. La ruta `EN_RECORRIDO` muestra debajo, en naranja, «Está en recorrido: se quitará al terminarlo». Las `RETIRO_PENDIENTE` no aparecen.
- Texto: «Debe marcar al menos una ruta. Se guardan la fecha, la hora y el administrador. La programación de los demás conductores no cambia.»
- No pide fecha: siempre es hoy.
- Validación en el cliente: «Escriba qué pasó» y «Indique al menos una ruta para asignar o quitar».
- `POST /conductores/{id}/reasignaciones`. Al responder `201`, actualizar el detalle con la `programacion` que trae la respuesta (sin otro GET), invalidar las keys de §2, notificación verde «Reasignación guardada» y cerrar.

### 4.2 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-25.1 | Conductor con programación de hoy | Reasigna con motivo y rutas | La programación cambia y aparece en el historial con hora y administrador |
| CA-25.2 | Motivo vacío | Guarda | «Escriba qué pasó», sin llamar a la API |
| CA-25.3 | Ruta en recorrido marcada para quitar | Guarda | Queda «Se retira al terminar el recorrido» |
| CA-25.4 | Reasignación guardada | Selecciona otro conductor | Su programación no cambió |
| CA-25.5 | Ninguna ruta marcada | Guarda | «Indique al menos una ruta para asignar o quitar» |

### 4.3 Historial

Tabla bajo el detalle de programación con `GET /reasignaciones?conductorId=&fecha=`: **Fecha y hora** (`formatearFechaHora`), **Administrador**, **Qué pasó**, **Asignadas** y **Quitadas** (códigos como insignias). Más reciente primero. Vacío: «Sin reasignaciones en esta fecha.»

## 5. Monitoreo (`/monitoreo`) — mockup 09

**HU-31** — Como administrador de la empresa, quiero ver en un mapa los buses que tienen un recorrido activo, con su vehículo, ruta, conductor y hora de la última posición, para tener visibilidad de la operación de la empresa durante el piloto.

### 5.1 Datos

- `GET /monitoreo/buses` con React Query, `refetchInterval: 4000`. **Pausar** el polling cuando la pestaña no está visible (`refetchIntervalInBackground: false` y respetar `document.visibilityState`) y al salir de la pantalla.
- Trazados: para cada `ruta.id` distinto que aparezca en `buses`, `GET /rutas/{id}` con la query `['ruta', id]` y `staleTime: Infinity` (no cambian entre consultas).

### 5.2 Pantalla

- Encabezado: «Monitoreo», subtítulo «Vista de solo lectura de los buses en recorrido».
- Aviso fijo arriba (fondo `#FFF4E5`, texto `#7A3E00`, ícono de información): **«Posición simulada.** Los buses se mueven según la hora sobre el recorrido principal de su ruta. No hay GPS real.» (CA-31.5).
- **Mapa** (`MapaBase`, 620 px de alto, columna principal): recorrido principal de cada ruta con su color (opacidad 0.6) y un marcador `bus` por elemento de `buses`, del color de su ruta. Los marcadores se **mueven** (actualiza su `setLngLat`, no los recrees). Clic en un bus → popup con número interno, placa, ruta, conductor y «Posición de las [hora]». Al cargar por primera vez, encuadrar a todos los trazados.
- **Panel derecho (~320 px)**: «Buses en recorrido (N)», una fila por bus con número interno y ruta en negrita y «[conductor] · [hora]» debajo. Clic en la fila → centra el mapa en ese bus y abre su popup.
- Pie del panel: «Actualizado a las [generadoEn]».
- Vacío (`buses: []`): el mapa se muestra igual, centrado en Popayán, y el panel dice «No hay buses en recorrido» (CA-31.3).
- Error de red durante el polling: no borrar lo último que se vio; mostrar una franja «No se pudo actualizar. Reintentando…».

### 5.3 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-31.1 | Recorridos activos (perfil demo: 2) | Abre Monitoreo | Ve cada bus sobre su ruta con vehículo, ruta, conductor y hora |
| CA-31.2 | Pantalla abierta | Pasan unos segundos | Los buses avanzan sin recargar la página |
| CA-31.3 | Ningún recorrido activo | Abre Monitoreo | «No hay buses en recorrido» |
| CA-31.4 | Un recorrido se finaliza | Siguiente actualización | Ese bus desaparece |
| CA-31.5 | — | Mira la pantalla | Aviso visible de posición simulada |

## 6. Recorridos de demostración (`/demo`)

No es una HU: es la herramienta de la decisión **D-16** para poder probar las reglas que dependen de recorridos activos sin la app del conductor.

### 6.1 Visibilidad

- Al cargar el layout, consultar una vez `GET /demo/recorridos?estado=ACTIVO`. Si responde **404**, el backend no está en perfil `demo`: **no** mostrar el ítem del menú y redirigir `/demo` a `/`. Si responde 200, mostrar en el menú, debajo de Monitoreo y separado por una línea, el ítem **Recorridos de demostración** con una insignia gris «Demo».
- La pantalla lleva un aviso: «Herramienta de demostración. Reemplaza a la app del conductor, que todavía no existe.»

### 6.2 Iniciar recorrido

Formulario en una tarjeta:

- **Conductor**: `Select` con los conductores **disponibles y sin recorrido activo** (`GET /conductores?disponible=true`, filtrando `enRecorrido == null`).
- **Ruta**: `Select` habilitado tras elegir conductor, con las rutas de su programación de **hoy** en estado `PENDIENTE` (`GET /conductores/{id}/programacion`). Si no tiene, texto «Este conductor no tiene rutas pendientes hoy. Asígnelas en Programación del día.» con enlace.
- **Vehículo**: `Select` con los vehículos **activos y sin recorrido** (`GET /vehiculos?activo=true`, filtrando `enRecorrido == null`), mostrados como «UM-104 · ABC123».
- Botón **Iniciar recorrido** → `POST /demo/recorridos`. Notificación verde «Recorrido iniciado: [conductor] en [ruta] con [vehículo]». Los `409` se muestran con su `mensaje`.

### 6.3 Recorridos

- Selector **Activos / Finalizados**.
- Tabla: **Conductor**, **Ruta**, **Vehículo**, **Inicio**, **Fin** (solo finalizados), **Acciones**: **Finalizar** en los activos (con confirmación «¿Finalizar el recorrido de [conductor] en [ruta]?»), que llama a `POST /demo/recorridos/{id}/finalizar`.
- Enlace rápido **Ver en el monitoreo**.

### 6.4 Pruebas de punta a punta que esta pantalla permite (lista de verificación manual)

1. Iniciar un recorrido → en Conductores aparece «En recorrido» y se bloquean eliminar y «No disponible» (D-05, D-08).
2. Con el vehículo en recorrido → en Vehículos se bloquean eliminar y desactivar (RN-26).
3. Con la ruta en recorrido → en Rutas se bloquea eliminar (CA-05.2).
4. Reasignar quitando la ruta en curso → queda «Se retira al terminar el recorrido»; finalizar el recorrido → la ruta sale de la programación (D-11).
5. El monitoreo muestra el bus nuevo y deja de mostrarlo al finalizarlo.

## 7. Pulido final del panel

- Revisar todas las pantallas a 360 px de ancho y a 1440 px.
- Títulos de pestaña del navegador por pantalla: «Conductores · UrbanMovil Admin», etc.
- Favicon con el ícono de bus.
- Estados de carga (esqueletos o cargador) en todas las listas y en el mapa.
- `npm run build` genera `dist/` sin errores; documentar en el README cómo servirlo (`npm run preview`).

## 8. Definición de terminado (Fase 3 y frontend)

- [ ] Programación del día con fecha en la URL, reordenamiento por botones, agregar y retirar, guardado y modo solo lectura en días pasados.
- [ ] Reasignación con opciones filtradas, validaciones y refresco con la respuesta del `POST`.
- [ ] Historial de reasignaciones.
- [ ] Monitoreo con polling cada 4 s, pausa en segundo plano, marcadores que se mueven, panel lateral y aviso de simulación.
- [ ] Recorridos de demostración visibles solo con el perfil `demo`.
- [ ] Todos los CA de §3.3, §4.2 y §5.3 y la lista de §6.4 probados a mano contra el backend en perfil `demo`.
- [ ] `npm run build` y `npm run lint` sin errores.
- [ ] README con un **guion de demo de 5 minutos**: login → rutas (ver y editar una) → conductores y vehículos → programación y reasignación → iniciar un recorrido de demo → monitoreo → finalizarlo.

## 9. Trazabilidad

| Pantalla | HU / decisión | Mockup |
| --- | --- | --- |
| Programación del día | HU-09, V-5 | 07 |
| Reasignación e historial | HU-25, D-11 | 08, 07 |
| Monitoreo | HU-31, D-16 | 09 |
| Recorridos de demostración | D-16, RN-25, D-05, D-08, D-11 | — |
