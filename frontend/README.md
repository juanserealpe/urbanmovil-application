# UrbanMovil · Panel web del administrador

Frontend del panel de administración (épica EP-01). React + TypeScript (Vite), Mantine, React Query, react-hook-form + zod.

Estado actual: **Fase 3 (panel completo)** — login, menú, conductores y vehículos (HU-01, HU-06, HU-07, HU-08, HU-28, HU-29, HU-30), rutas con editor sobre mapa (HU-02 a HU-05), programación del día (HU-09), reasignaciones por eventualidad (HU-25), monitoreo (HU-31) y recorridos de demostración (D-16).

### Pantallas

| Ruta | Pantalla |
| --- | --- |
| `/` | Redirige a `/rutas` |
| `/rutas` | Lista de rutas con buscador, vista previa en mapa y eliminación (mockup 02) |
| `/rutas/nueva` · `/rutas/:id` | Editor de ruta: datos, trazado del recorrido principal y de los caminos alternos (mockup 03) |
| `/conductores` · `/vehiculos` | Altas, edición, disponibilidad / estado (Fase 1) |
| `/programacion?fecha=&conductor=` | Programación del día por conductor, reasignación e historial (mockups 07 y 08) |
| `/monitoreo` | Mapa de buses en recorrido con polling cada 4 s (mockup 09) |
| `/demo` | Recorridos de demostración. **Solo con el backend en perfil `demo`**; si no, el ítem no aparece en el menú y `/demo` redirige a `/` |

## Requisitos

- Node.js LTS (20 o superior) y npm.
- El backend de `../backend` corriendo en `http://localhost:8080` (necesita Java y Docker; ver `../backend/README.md`).

## Cómo arrancar

1. Backend con datos de prueba (en otra terminal):

   ```bash
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=demo'
   ```

2. Frontend:

   ```bash
   cd frontend
   npm install
   cp .env.example .env   # opcional: VITE_API_URL=/api/v1 es el valor por defecto
   npm run dev
   ```

3. Abrir `http://localhost:5173`.

### Servir el build

```bash
npm run build     # genera dist/
npm run preview   # sirve dist/ en http://localhost:4173
```

`vite.config.ts` configura el mismo proxy de `/api` también para `preview`, así que con el backend en `:8080` el build funciona igual que en desarrollo. En un servidor real, sirva `dist/` detrás de uno que reenvíe `/api` al backend y devuelva `index.html` para cualquier ruta (la cookie de sesión necesita el mismo origen).

Vite hace de proxy de `/api` hacia `http://localhost:8080` (`vite.config.ts`), así que el navegador ve frontend y API en el mismo origen y la cookie de sesión `JSESSIONID` funciona sin configurar CORS. No se guarda ningún token en el navegador.

### Credenciales demo

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@urbanmovil.co` | `Admin12345` |
| Conductores (no entran al panel) | ver `FRONTEND-API.md` §9 | `Conductor123` |

### Scripts

| Comando | Qué hace |
| --- | --- |
| `npm run dev` | Servidor de desarrollo en el puerto 5173 |
| `npm run build` | Chequeo de tipos (`tsc -b`) y build de producción en `dist/` |
| `npm run preview` | Sirve `dist/` en `http://localhost:4173` (después de `npm run build`) |
| `npm run lint` | oxlint |

## Estructura de carpetas

```
src/
├─ main.tsx         // punto de entrada: CSS de Mantine y <App />
├─ global.css
├─ app/             // App, router, providers, queryClient, theme, invalidaciones
├─ api/             // client.ts (fetch + ApiError), tipos.ts y un archivo por recurso
├─ auth/            // useSesion, RequireAuth, EscuchaSesionExpirada, LoginPage
├─ layout/          // AppLayout: menú lateral (AppShell + Burger en móvil)
├─ components/      // piezas reutilizables: chips, encabezado, estado vacío, confirmación…
├─ mapa/           // MapaBase (MapLibre), coordenadas.ts (lat/lng ↔ [lng, lat]), marcadores, config
├─ features/
│  ├─ conductores/  // ConductoresPage, ConductorForm (drawer), DisponibilidadModal, hooks
│  ├─ vehiculos/    // VehiculosPage, VehiculoForm (drawer), DesactivarModal, hooks
│  ├─ rutas/        // RutasPage, VistaPreviaRuta, hooks, dibujoRuta, constantes
│  │  └─ editor/    // EditorRutaPage, trazado.ts (reducer), validacion.ts (zod + errores), barra y tarjetas
│  ├─ programacion/ // ProgramacionPage, DetalleProgramacion, ReasignarModal, HistorialReasignaciones, hooks
│  ├─ monitoreo/    // MonitoreoPage, hooks (polling + trazados)
│  └─ demo/         // DemoPage, IniciarRecorridoForm, hooks (incluye useModoDemo)
└─ utils/           // errores.ts, fechas.ts, motivos.ts, contrasena.ts
```

Decisiones clave:

- **Errores de la API**: `utils/errores.ts → manejarErrorFormulario` reparte `campos` bajo cada input; lo que no es de un campo va a una alerta roja del formulario (`root.servidor` de react-hook-form); `REGLA_NEGOCIO` y `NO_ENCONTRADO` van a notificación con el `mensaje` del backend; `ERROR_INTERNO` muestra un mensaje genérico, salvo cuando el backend no responde («No se pudo conectar con el servidor…»).
- **Sesión vencida**: el cliente dispara el evento `sesion-expirada` ante un `401 NO_AUTENTICADO`. `EscuchaSesionExpirada` (raíz del router) limpia la caché, avisa y lleva a `/login`. No actúa si ya se está en `/login` ni si en esta pestaña nunca hubo sesión (en ese caso `RequireAuth` redirige sin aviso).
- **Caché**: `['me']`, `['conductores', filtros]`, `['vehiculos', filtros]`, `['rutas', { q }]`, `['ruta', id]`, `['programaciones', fecha]`, `['programacion', conductorId, fecha]`, `['reasignaciones', filtros]`, `['monitoreo']`, `['demo', 'recorridos', estado]`. Las mutaciones invalidan por prefijo. Las invalidaciones que cruzan pantallas están juntas en `app/invalidaciones.ts` (tras programación, reasignación, recorrido y conductor), para que no se olvide ninguna.
- **Programación del día**: fecha y conductor viven en la URL. Los cambios (agregar, retirar, subir/bajar) son locales hasta **Guardar programación**, que envía los ids en el orden visible; el borrador está atado a su conductor y fecha, y cambiar de cualquiera de los dos con cambios pendientes pide confirmación. Fechas pasadas: solo lectura. **Reasignar** solo aparece hoy y se deshabilita con cambios sin guardar (el `POST` cambia el servidor y dejaría el borrador desfasado); su respuesta actualiza el detalle sin otro GET.
- **Monitoreo**: `refetchInterval` de 4 s que se apaga con la pestaña oculta (`useDocumentVisibility` + `refetchIntervalInBackground: false`) y al salir de la pantalla. Los trazados se piden una vez (`staleTime: Infinity`, `useQueries` con `combine`). Los marcadores se **mueven** (`setLngLat`); el popup se actualiza en el lugar. Si falla una actualización se conserva lo último visto y se avisa «No se pudo actualizar. Reintentando…».
- **Modo demo**: `useModoDemo` reutiliza la query `['demo', 'recorridos', 'ACTIVO']`. Solo un **404** significa «sin perfil demo» (oculta el menú y redirige); otro error muestra el error en `/demo` sin redirigir.
- **Mapa**: `maplibre-gl` directo con el estilo de OpenFreeMap (`tiles.openfreemap.org/styles/liberty`), centrado en Popayán. No se usa Leaflet ni Geoman. `MapaBase` crea el mapa una vez y lo destruye al desmontar; las líneas van en una sola fuente GeoJSON (capa sólida + capa punteada) que se actualiza con `setData`; los marcadores se sincronizan por `id`. La API usa `{ lat, lng }` y MapLibre `[lng, lat]`: la conversión vive **solo** en `src/mapa/coordenadas.ts`.
- **Editor de ruta**: el trazado (principal, alternos, cuál está activo) es un reducer puro (`editor/trazado.ts`); los datos (nombre, código, color) van en react-hook-form. Al guardar se valida todo con zod con los mismos mensajes del backend. En el `PUT` se envían **todos** los alternos (con `id` los existentes, sin `id` los nuevos); el que se elimina en la UI simplemente no se envía. Los errores del backend `recorridoPrincipal` / `alternos[i].…` se muestran en la tarjeta correspondiente y además en la alerta general.
- Un clic sobre un marcador también llega como clic al mapa en MapLibre; `MapaBase` lo descarta para no agregar puntos encima de un vértice. Con el mapa editable se desactiva el zoom con doble clic.

## Lista de verificación manual (Fase 1)

Con el backend en perfil `demo` recién arrancado.

### Sesión (HU-01)

- [ ] CA-01.1 Credenciales válidas → entra a `/` (redirige a `/conductores`).
- [ ] CA-01.1 Abrir `/vehiculos` sin sesión → va a `/login`; tras ingresar vuelve a `/vehiculos`.
- [ ] CA-01.2 Contraseña incorrecta → alerta roja «Credenciales inválidas» sobre el botón; no entra.
- [ ] CA-01.3 Correo de un conductor (`andres@…` / `Conductor123`) → igual que CA-01.2.
- [ ] Campo vacío → «Escriba el correo» / «Escriba la contraseña» sin llamar a la API (pestaña Red vacía).
- [ ] Mientras se envía → botón en estado de carga.
- [ ] Recargar la página con sesión → sigue dentro (restauración con `GET /auth/me`).
- [ ] Entrar a `/login` con sesión → redirige a `/`.
- [ ] Reiniciar el backend y hacer cualquier acción → «Su sesión venció. Inicie sesión de nuevo.» y va al login (una sola vez).
- [ ] Cerrar sesión → vuelve al login; volver atrás no muestra datos.
- [ ] Backend apagado → «No se pudo conectar con el servidor. Verifique que el backend esté encendido.»
- [ ] Ventana de menos de 768 px → menú con hamburguesa; a 360 px nada se corta (tablas con scroll horizontal).

### Conductores (HU-06, HU-07, HU-08)

- [ ] CA-06.1 Registrar con datos completos → aparece como «Disponible».
- [ ] CA-06.2 Identificación o correo repetidos → mensaje del backend bajo el campo.
- [ ] CA-06.3 Campo vacío → mensaje bajo el campo sin llamar a la API.
- [ ] Botón **Generar** → contraseña aleatoria de 12 caracteres visible.
- [ ] CA-07.1 Editar y guardar → la lista muestra los cambios; contraseña vacía no la cambia.
- [ ] CA-07.2 Eliminar un conductor sin recorrido (Carlos) y confirmar → desaparece.
- [ ] CA-07.3 Andrés/Laura (en recorrido) → Eliminar deshabilitado con tooltip «Tiene un recorrido activo».
- [ ] CA-08.1 Marcar a Carlos no disponible con motivo → tabla muestra «No disponible» y el motivo.
- [ ] CA-08.2 No disponible sin motivo → «Seleccione el motivo».
- [ ] Motivo «Otro» sin detalle → «Describa el motivo».
- [ ] CA-08.3 Marcar a Diana disponible → tabla muestra «Disponible».
- [ ] CA-08.4 Abrir disponibilidad de Andrés → «No disponible» deshabilitado con el aviso de recorrido.
- [ ] Buscador (espera 300 ms) y filtro Todos / Disponibles / No disponibles; contador «N conductores».

### Vehículos (HU-28, HU-29, HU-30)

- [ ] CA-28.1 Registrar `um-500` / `xyz-12 3` → aparece «Activo» como `UM-500` / `XYZ123`.
- [ ] CA-28.2 Placa o número interno repetidos → mensaje bajo el campo.
- [ ] CA-28.3 Campo vacío → mensaje bajo el campo sin llamar a la API.
- [ ] CA-29.1 Editar y guardar → la lista muestra los cambios.
- [ ] CA-29.2 Eliminar un vehículo sin recorrido y confirmar → desaparece.
- [ ] CA-29.3 `UM-104` / `UM-305` (en recorrido) → Eliminar deshabilitado.
- [ ] CA-29.4 Cancelar el diálogo de eliminar → no se elimina.
- [ ] CA-30.1 Desactivar con motivo → chip «Inactivo» con motivo y «desde [fecha]».
- [ ] CA-30.2 Desactivar sin motivo → «Escriba el motivo de la desactivación».
- [ ] CA-30.3 Vehículo en recorrido → Desactivar deshabilitado.
- [ ] CA-30.4 Reactivar `UM-410` → chip «Activo», sin motivo.

## Lista de verificación manual (Fase 2)

Con el backend en perfil `demo`.

### Lista de rutas (HU-04, HU-05)

- [ ] `/` redirige a `/rutas` y el menú marca «Rutas» (también en `/rutas/nueva` y `/rutas/:id`).
- [ ] Tabla: insignia con el código sobre el color, nombre, «Editada [fecha]», «N puntos», alternos, «Asignada hoy a» (o «Sin asignar») y chip de estado.
- [ ] Buscador «Buscar por código o nombre» (espera 300 ms) y contador «N rutas».
- [ ] Al entrar se selecciona la primera ruta y se ve su vista previa (300 px) con principal sólido, alternos punteados naranja, pines I y F y la leyenda.
- [ ] Clic (o Enter) en otra fila → cambia la vista previa; los botones de la fila no cambian la selección.
- [ ] CA-03.2 Ruta con 2 alternos → la vista previa muestra los dos punteados, con la leyenda.
- [ ] CA-05.1 Eliminar una ruta sin recorridos activos y confirmar «Eliminar ruta» → desaparece de la lista.
- [ ] CA-05.2 Ruta con recorridos activos → Eliminar deshabilitado con tooltip «Tiene recorridos activos»; si llega un 409 se muestra en notificación.
- [ ] CA-05.3 Cancelar el diálogo «¿Eliminar la ruta [código]?» → no se elimina.
- [ ] Sin rutas → «Todavía no hay rutas. Cree la primera con Nueva ruta.»

### Editor de ruta (HU-02, HU-03, HU-04)

- [ ] Clic en el mapa agrega puntos al final del trazado activo; arrastrar un vértice (o los pines I/F) lo mueve; «Deshacer punto» quita el último; «Borrar trazado» lo vacía (pide confirmación con más de 2 puntos).
- [ ] CA-02.5 Pines I y F en el primer y último punto, en el orden de los clics.
- [ ] Trazado activo con vértices; inactivos sin vértices y tenues; alternos siempre punteados.
- [ ] Chips «N puntos», «Inicio marcado», «Final marcado» en verde con 2 o más puntos; «Falta trazar» en naranja si no.
- [ ] Color: muestras de la paleta del backend; al cambiarlo, la línea principal cambia de color.
- [ ] CA-02.1 Nombre nuevo + recorrido trazado → «Ruta creada», vuelve a `/rutas` con esa ruta seleccionada en la vista previa.
- [ ] CA-02.2 Sin recorrido → «Falta trazar el recorrido principal», sin llamar a la API (pestaña Red).
- [ ] CA-02.3 Nombre de otra ruta → «Ya existe una ruta con este nombre» bajo el nombre.
- [ ] CA-02.4 Recorrido de un solo punto → «La ruta debe tener punto de inicio y punto final».
- [ ] Alterno sin nombre o con menos de dos puntos → mensaje en su tarjeta y en la alerta general.
- [ ] CA-03.1 Agregar un alterno, trazarlo y guardar → se ve punteado, distinto del principal.
- [ ] «Agregar camino alterno» se deshabilita al llegar a 10.
- [ ] CA-04.1 Editar nombre o mover puntos y guardar → «Cambios guardados»; la lista y la vista previa muestran la versión nueva.
- [ ] Al abrir una ruta existente, el mapa se encuadra al recorrido principal.
- [ ] CA-04.2 Borrar el recorrido principal y guardar → «Falta trazar el recorrido principal»; no se guarda.
- [ ] CA-04.3 Ruta con 2 alternos: eliminar uno y guardar → al reabrirla queda solo uno.
- [ ] Cancelar con cambios → pide confirmación; sin cambios vuelve directo a `/rutas`.
- [ ] `/rutas/999999` → «La ruta no existe» con botón para volver.
- [ ] Ventana angosta → el panel queda debajo del mapa.

## Lista de verificación manual (Fase 3)

Con el backend en perfil `demo` recién arrancado (hoy: Andrés `[R1, R2]` en recorrido en R1, Laura `[R2]` en recorrido en R2, Carlos `[R1, R2]`, Diana no disponible).

### Programación del día (HU-09)

- [ ] Fecha por defecto hoy; al cambiarla o elegir conductor, la URL queda `?fecha=…&conductor=…` y recargar la conserva.
- [ ] Tarjetas: nombre, «N rutas programadas», chip «En recorrido · R1» (Andrés) y «No disponible» (Diana); la seleccionada con borde azul.
- [ ] CA-09.1 Conductor sin rutas (Diana): agregar R2 y R1, guardar, recargar → R2, R1 en ese orden.
- [ ] CA-09.2 Retirar una ruta pendiente y guardar → ya no aparece.
- [ ] CA-09.3 Andrés, R1 en recorrido → chip «En recorrido», candado, «No se puede retirar hasta que termine», sin botón de retirar.
- [ ] CA-09.4 «Agregar ruta» no ofrece las rutas ya asignadas.
- [ ] CA-09.5 Subir/bajar y guardar → queda el nuevo orden.
- [ ] Con cambios: franja «Tiene cambios sin guardar» + **Descartar**; cambiar de conductor o de fecha pide confirmación.
- [ ] Fecha pasada → solo lectura con «Las programaciones de días pasados no se pueden modificar.»
- [ ] **Reasignar por eventualidad** solo visible hoy.

### Reasignación (HU-25)

- [ ] El modal ofrece para asignar solo las rutas que no tiene y para quitar solo las que tiene (sin las `RETIRO_PENDIENTE`); la de recorrido dice «Está en recorrido: se quitará al terminarlo».
- [ ] CA-25.1 Motivo + rutas → «Reasignación guardada»; la programación cambia y aparece en el historial con hora y administrador.
- [ ] CA-25.2 Motivo vacío → «Escriba qué pasó», sin llamar a la API.
- [ ] CA-25.3 Quitar a Andrés la R1 (en recorrido) → queda «Se retira al terminar el recorrido».
- [ ] CA-25.4 Seleccionar otro conductor → su programación no cambió.
- [ ] CA-25.5 Ninguna ruta marcada → «Indique al menos una ruta para asignar o quitar».
- [ ] Historial vacío → «Sin reasignaciones en esta fecha.»

### Monitoreo (HU-31)

- [ ] CA-31.5 Aviso «Posición simulada…» visible.
- [ ] CA-31.1 Se ven 2 buses sobre sus rutas; clic en un bus → popup con número interno, placa, ruta, conductor y «Posición de las [hora]».
- [ ] CA-31.2 Pasan unos segundos → los buses avanzan sin recargar; «Actualizado a las …» cambia.
- [ ] Clic en una fila del panel → centra el mapa en ese bus y abre su popup.
- [ ] Pestaña oculta → no hay peticiones a `/monitoreo/buses` (pestaña Red); al volver se reanudan.
- [ ] Apagar el backend → franja «No se pudo actualizar. Reintentando…» y los buses siguen donde estaban.
- [ ] CA-31.3 Sin recorridos activos → mapa centrado en Popayán y «No hay buses en recorrido».
- [ ] CA-31.4 Finalizar un recorrido → en la siguiente actualización ese bus desaparece.

### Recorridos de demostración (§6.4)

- [ ] Con perfil demo: ítem «Recorridos de demostración» con insignia «Demo», separado por una línea. Sin perfil demo: no aparece y `/demo` redirige a `/`.
- [ ] Conductor: solo disponibles sin recorrido; Ruta: solo pendientes de hoy (o el texto con enlace a Programación); Vehículo: activos sin recorrido, como «UM-118 · ABC123».
- [ ] 1. Iniciar un recorrido (Carlos, R1, UM-118) → «Recorrido iniciado: …»; en Conductores, Carlos «En recorrido» con eliminar y «No disponible» bloqueados.
- [ ] 2. En Vehículos, UM-118 con eliminar y desactivar bloqueados.
- [ ] 3. En Rutas, R1 con eliminar bloqueado.
- [ ] 4. Reasignar a Carlos quitando R1 → «Se retira al terminar el recorrido»; finalizarlo → R1 sale de su programación.
- [ ] 5. El monitoreo muestra el bus nuevo y deja de mostrarlo al finalizarlo.
- [ ] Finalizados: columna **Fin**; Finalizar pide «¿Finalizar el recorrido de [conductor] en [ruta]?».

### Pulido

- [ ] Todas las pantallas a 360 px y a 1440 px.
- [ ] Título de pestaña por pantalla («Conductores · UrbanMovil Admin»…) y favicon de bus.
- [ ] `npm run build` y `npm run lint` sin errores.

## Guion de demo (5 minutos)

Backend en perfil `demo` recién arrancado, frontend en `http://localhost:5173`.

1. **Login (0:00–0:30)** — Entrar con `admin@urbanmovil.co` / `Admin12345`. Se abre **Rutas**.
2. **Rutas (0:30–1:30)** — Mostrar la tabla (R1 y R2 «N recorridos activos», eliminar bloqueado) y la vista previa de R3 con sus 2 alternos punteados y la leyenda. **Editar R3**: arrastrar un vértice, agregar un punto al final con un clic, guardar → «Cambios guardados» y la vista previa ya muestra la versión nueva.
3. **Conductores y vehículos (1:30–2:15)** — En Conductores, Andrés y Laura «En recorrido» (eliminar y «No disponible» bloqueados con tooltip). En Vehículos, UM-104 y UM-305 en recorrido; UM-410 inactivo con su motivo.
4. **Programación y reasignación (2:15–3:30)** — En **Programación del día**, elegir Carlos (`R1, R2`): bajar R1, guardar. Elegir Andrés (`R1, R2`, en recorrido en R1): R1 con candado. **Reasignar por eventualidad**: «Andrés reportó falla mecánica», asignarle R3 y quitarle R1 → R1 queda «Se retira al terminar el recorrido» y la reasignación aparece en el historial con hora y administrador.
5. **Iniciar recorrido de demo (3:30–4:00)** — En **Recorridos de demostración**, iniciar Carlos · R2 · UM-118 → «Recorrido iniciado…».
6. **Monitoreo (4:00–4:40)** — Señalar el aviso de posición simulada, los 3 buses avanzando y «Actualizado a las…». Clic en el bus de Carlos en el panel → el mapa se centra y abre su popup.
7. **Finalizar (4:40–5:00)** — Volver a Recorridos de demostración, finalizar el de Andrés → en Programación, R1 sale de su lista (estaba en retiro pendiente) y en Monitoreo su bus desaparece en la siguiente actualización.
