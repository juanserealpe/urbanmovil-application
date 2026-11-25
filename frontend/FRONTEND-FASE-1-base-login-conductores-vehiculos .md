# UrbanMovil · Frontend administrativo · Fase 1 de 3 — Base, login, conductores y vehículos

> Documento de implementación para el agente de desarrollo. Implementar **solo esta fase**. Las fases 2 y 3 vienen en archivos aparte. El contrato de la API está en **`FRONTEND-API.md`** (fuente de verdad para endpoints, tipos, mensajes y códigos de error): léelo completo antes de empezar. Si este documento y `FRONTEND-API.md` se contradicen en algo de la API, manda `FRONTEND-API.md`; si se contradicen en algo de pantalla o de negocio, **detente y pregunta**.

## 1. Contexto

UrbanMovil es una plataforma de transporte público para una empresa de buses de Popayán (Colombia). Este frontend es el **panel web del administrador** (épica EP-01). El backend Spring Boot ya está terminado. Es un **MVP académico**: prioriza que funcione bien, se vea ordenado y respete las reglas de negocio, por encima de optimizaciones.

| Fase | HU | Pantallas |
| --- | --- | --- |
| **1 (este archivo)** | HU-01, HU-06, HU-07, HU-08, HU-28, HU-29, HU-30 | Base del proyecto, login, menú lateral, conductores y vehículos |
| 2 | HU-02 a HU-05 | Rutas y editor de ruta con mapa |
| 3 | HU-09, HU-25, HU-31 | Programación del día, reasignaciones, monitoreo simulado y recorridos de demostración |

Mockups de referencia (los comparte el equipo): «UrbanMovil — Mockups MVP Administrativo (Web)». Pantallas 01, 04, 05 y 06 corresponden a esta fase. No hay mockup de vehículos: se construye con el mismo patrón que conductores (§8).

## 2. Proyecto existente y stack

El proyecto **ya existe** (creado con `npm create vite@latest ... --template react-ts`) y ya tiene instaladas estas librerías. Trabaja sobre él; no lo regeneres ni cambies de librerías.

| Uso | Librería |
| --- | --- |
| UI | React + TypeScript (Vite) |
| Componentes | Mantine: `@mantine/core`, `@mantine/hooks`, `@mantine/notifications`, `@mantine/modals`, `@mantine/dates` + `dayjs` |
| Rutas de la app | `react-router` |
| Datos del servidor | `@tanstack/react-query` |
| Formularios | `react-hook-form` + `zod` + `@hookform/resolvers` |
| Mapa (fases 2 y 3) | `maplibre-gl` |
| Dev | `postcss`, `postcss-preset-mantine`, `postcss-simple-vars` |

Reglas:

- **Prohibido** añadir: axios, Redux/Zustand, Tailwind, MUI, Leaflet, Google Maps, librerías de drag & drop, librerías de tablas pesadas. Lo que falte se resuelve con Mantine o código propio.
- `@geoman-io/maplibre-geoman-free` está instalado, pero la Fase 2 traza con `maplibre-gl` directamente. Desinstálalo en la Fase 2 si no se usa.
- TypeScript estricto (`strict: true`). Sin `any` salvo casos justificados con comentario.
- Textos de la interfaz en **español**.

## 3. Configuración inicial

### 3.1 Vite y API

`vite.config.ts` ya tiene un proxy de `/api` hacia `http://localhost:8080`. Consérvalo. La URL base del cliente es:

```ts
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1';
```

Así el navegador ve frontend y API en el mismo origen (`localhost:5173`) y la cookie de sesión funciona sin configuración extra. Crea `.env.example` con `VITE_API_URL=/api/v1`.

### 3.2 Mantine

- `postcss.config.cjs` según la guía oficial de Mantine (`postcss-preset-mantine` y `postcss-simple-vars` con los breakpoints de Mantine).
- Importar los CSS de `@mantine/core`, `@mantine/notifications` y `@mantine/dates` en `main.tsx`.
- `MantineProvider` con un tema propio (`src/app/theme.ts`):

| Token | Valor |
| --- | --- |
| Color primario | Azul `#1F4FD8` (generar escala de 10 tonos con este como tono 6) |
| Texto principal / secundario | `#12161C` / `#4B5563` |
| Fondo de la app | `#F6F7F9` |
| Bordes | `#E3E6EB` |
| Éxito | texto `#0E5A2F` sobre `#DDF3E5` |
| Aviso | texto `#7A3E00` sobre `#FFF4E5` |
| Error | `#B42318` |
| Radio | 10 px en inputs y botones, 14 px en tarjetas |
| Tipografía | Inter (cuerpo) y Space Grotesk (títulos), desde Google Fonts en `index.html`; si falla, la del sistema |

- `Notifications` (posición arriba a la derecha) y `ModalsProvider`.
- `dayjs` con plugins `utc`, `timezone` y `customParseFormat`, y locale `es`. Zona del negocio: `America/Bogota`.

### 3.3 React Query

`QueryClient` con `retry: false` para errores 4xx, `refetchOnWindowFocus: false` y `staleTime` de 30 s por defecto.

## 4. Estructura de carpetas

```
src/
├─ main.tsx
├─ app/          // App.tsx, router.tsx, providers.tsx, theme.ts
├─ api/          // client.ts, tipos.ts y un archivo por recurso: auth.ts, conductores.ts, vehiculos.ts…
├─ auth/         // useSesion, RequireAuth, LoginPage
├─ layout/       // AppLayout (menú lateral + contenido)
├─ components/   // piezas reutilizables: EstadoVacio, ConfirmarEliminar, BadgeDisponibilidad, BadgeEnRecorrido, BuscadorConFiltros…
├─ features/
│  ├─ conductores/   // ConductoresPage, ConductorForm (drawer), DisponibilidadModal, hooks de queries
│  ├─ vehiculos/     // VehiculosPage, VehiculoForm, DesactivarModal, hooks
│  ├─ rutas/         // Fase 2
│  ├─ programacion/  // Fase 3
│  ├─ monitoreo/     // Fase 3
│  └─ demo/          // Fase 3
├─ mapa/         // Fase 2
└─ utils/        // fechas.ts, errores.ts
```

## 5. Capa de API

### 5.1 `api/tipos.ts`

Copia **todos** los tipos de la sección 4 de `FRONTEND-API.md` (también los de rutas, programación, monitoreo y demo, aunque se usen después) y los de errores de la sección 2 (`ErrorResponse`, `CampoError`, `CodigoError`).

### 5.2 `api/client.ts`

Implementa el cliente de la sección 8 de `FRONTEND-API.md` con estos ajustes:

- `BASE_URL` como en §3.1 (no `http://localhost:8080`).
- No mandes `Content-Type` cuando no hay cuerpo.
- Si la respuesta no es JSON (p. ej. el backend apagado o error del proxy), lanza `ApiError` con `codigo: 'ERROR_INTERNO'` y el mensaje «No se pudo conectar con el servidor. Verifique que el backend esté encendido.»
- Ante `401 NO_AUTENTICADO` dispara el evento `sesion-expirada`.

### 5.3 Un archivo por recurso

Funciones tipadas para cada endpoint (`listarConductores`, `crearConductor`, `modificarConductor`, `eliminarConductor`, `cambiarDisponibilidad`, `listarVehiculos`, `crearVehiculo`, `modificarVehiculo`, `eliminarVehiculo`, `cambiarEstadoVehiculo`, `login`, `logout`, `me`). Construye los query strings con `URLSearchParams` y omite los parámetros vacíos.

### 5.4 Hooks de React Query

En cada feature, hooks como `useConductores(filtros)`, `useCrearConductor()`, etc.

| Query key | Se invalida después de |
| --- | --- |
| `['me']` | login, logout |
| `['conductores', filtros]` | crear, modificar, eliminar, cambiar disponibilidad de un conductor |
| `['vehiculos', filtros]` | crear, modificar, eliminar, cambiar estado de un vehículo |

Invalida por prefijo (`['conductores']`) para refrescar todas las variantes de filtros.

### 5.5 `utils/errores.ts`

Una función que recibe un `ApiError` y el `setError` de react-hook-form:

1. `VALIDACION` o `DATO_REPETIDO` con `campos` → `setError(campo, { message })` por cada uno; los campos que no existan en el formulario se juntan en un error general.
2. `campos` vacío → error general del formulario (una `Alert` roja arriba de los botones).
3. `REGLA_NEGOCIO` → notificación roja con `mensaje`.
4. `ERROR_INTERNO` u otro → notificación roja «Ocurrió un error inesperado. Intente de nuevo.»

Úsala en **todos** los formularios. Las acciones sin formulario (eliminar, reactivar) muestran `mensaje` en una notificación.

## 6. Sesión, login y menú (HU-01)

**HU-01** — Como administrador de la empresa, quiero iniciar sesión en la aplicación web con mis credenciales, para acceder solo a las funciones de administración.

### 6.1 Rutas de la app

| Ruta | Pantalla | Protegida |
| --- | --- | --- |
| `/login` | Inicio de sesión | No |
| `/` | Redirige a `/conductores` (Fase 2 la cambiará a `/rutas`) | Sí |
| `/conductores` | Conductores | Sí |
| `/vehiculos` | Vehículos | Sí |
| `/rutas`, `/programacion`, `/monitoreo` | Página provisional «Disponible en la siguiente fase» | Sí |
| `*` | «Página no encontrada» con enlace al inicio | — |

### 6.2 Sesión

- `useSesion()` usa la query `['me']` (`GET /auth/me`). Mientras carga, se muestra un cargador centrado.
- `RequireAuth`: si `me` responde 401, redirige a `/login` guardando la ruta de origen para volver después del login.
- Un listener del evento `sesion-expirada` limpia la caché de React Query, muestra la notificación «Su sesión venció. Inicie sesión de nuevo.» y navega a `/login`. No debe disparar en bucle si ya se está en `/login`.
- Si hay sesión y se entra a `/login`, redirige a `/`.

### 6.3 Pantalla de login (mockup 01)

Tarjeta centrada de 420 px máx.: logo (cuadro azul con ícono de bus), «UrbanMovil», subtítulo «Administración de la empresa», título «Iniciar sesión», campos **Correo** y **Contraseña** (con botón de mostrar), botón **Ingresar** de ancho completo y texto pequeño «Solo para administradores».

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-01.1 | Credenciales válidas | Toca Ingresar | Entra a `/` (o a la ruta que intentaba abrir) |
| CA-01.2 | Contraseña incorrecta | Toca Ingresar | Alerta roja «Credenciales inválidas» sobre el botón; no entra |
| CA-01.3 | Correo de un conductor | Toca Ingresar | Igual que CA-01.2 |
| — | Campo vacío | Toca Ingresar | Mensaje bajo el campo («Escriba el correo» / «Escriba la contraseña») sin llamar a la API |
| — | Mientras se envía | — | Botón en estado de carga y deshabilitado |

### 6.4 Menú lateral (`AppLayout`, como en los mockups)

- Columna izquierda de ~240 px, fondo blanco y borde derecho: logo y «UrbanMovil / Administración»; navegación con ícono y texto: **Rutas**, **Conductores**, **Vehículos**, **Programación del día**, **Monitoreo**. El ítem activo con fondo `#E4EBFC` y texto azul. Al pie: nombre y correo del administrador (`me`) y botón **Cerrar sesión** (`POST /auth/logout` → limpiar caché → `/login`).
- En pantallas de menos de 768 px el menú se convierte en un `AppShell` con botón hamburguesa (Mantine `AppShell` + `Burger`).
- Encabezado de cada página: título grande (Space Grotesk, 28 px), subtítulo gris y acciones a la derecha.
- Íconos: SVG en línea propios o `@tabler/icons-react` (es la única librería extra permitida, la recomienda Mantine).

## 7. Conductores (HU-06, HU-07, HU-08) — mockups 04, 05 y 06

**HU-06** — Como administrador de la empresa, quiero registrar un conductor con sus datos básicos y credenciales de acceso, para que pueda usar la aplicación del conductor.
**HU-07** — Como administrador de la empresa, quiero modificar los datos de un conductor o eliminarlo, para mantener actualizado el personal habilitado.
**HU-08** — Como administrador de la empresa, quiero marcar a un conductor como disponible o no disponible e indicar el motivo, para que solo los conductores disponibles puedan operar.

### 7.1 Lista (`/conductores`)

- Encabezado: «Conductores», subtítulo «Personal habilitado para usar la app del conductor», botón primario **Registrar conductor**.
- Filtros: buscador «Nombre, identificación o correo» (con espera de 300 ms antes de consultar) y selector segmentado **Todos / Disponibles / No disponibles** (`disponible`). Contador «N conductores».
- Tabla (con scroll horizontal en pantallas angostas): **Conductor** (nombre en negrita y «CC identificación» debajo), **Correo**, **Teléfono**, **Disponibilidad**, **Rutas de hoy** (`rutasHoy` separados por coma o «—»), **Acciones**.
- Disponibilidad: chip verde «Disponible» o chip naranja «No disponible» con el motivo en texto (Incapacidad, Vacaciones, Permiso, Otro: detalle). Si `enRecorrido`, chip azul con ícono de bus «En recorrido · R1».
- Acciones (botones de ícono con `aria-label` y tooltip): **Editar**, **Cambiar disponibilidad**, **Eliminar**.
- Estado vacío: «No hay conductores registrados» (o «Ningún conductor coincide con la búsqueda» si hay filtros).
- Nota al pie: «Un conductor en recorrido no se puede eliminar ni marcar como no disponible hasta que termine.»

### 7.2 Registrar / editar (panel lateral derecho, mockup 05)

`Drawer` de 460 px con título «Registrar conductor» o «Editar conductor».

| Campo | Validación en el formulario (zod) |
| --- | --- |
| Nombre completo | Obligatorio, máx. 120 |
| Número de identificación | Obligatorio, máx. 20 |
| Correo | Obligatorio, formato de correo, máx. 120. Ayuda: «Será su usuario en la app del conductor» |
| Teléfono | Obligatorio, máx. 20 |
| Contraseña inicial | Al registrar: obligatoria, mínimo 8, con botón **Generar** que crea una aleatoria de 12 caracteres y la muestra. Al editar: opcional, con ayuda «Déjela vacía para no cambiarla» |

- Aviso azul al registrar: «El conductor queda disponible por defecto. Todos los campos son obligatorios.»
- Botones **Cancelar** y **Registrar** / **Guardar cambios**. Al guardar bien: cerrar, notificación verde «Conductor registrado» / «Cambios guardados» y refrescar la lista.
- Errores con `utils/errores.ts`: por ejemplo, `409 DATO_REPETIDO` en `identificacion` queda bajo ese campo.

### 7.3 Disponibilidad (modal, mockup 06)

Modal «Disponibilidad de [nombre]»:

- Estado: dos opciones grandes **Disponible** / **No disponible** (`Radio` o `SegmentedControl`).
- Si «No disponible»: **Motivo** obligatorio con 4 opciones (Incapacidad, Vacaciones, Permiso, Otro) y **Detalle** (textarea, máx. 300), obligatorio solo con «Otro».
- Texto de ayuda: «Mientras esté no disponible, no podrá iniciar recorridos.»
- Si el conductor tiene `enRecorrido`, la opción «No disponible» aparece deshabilitada con el texto «En recorrido: no se puede marcar como no disponible hasta que termine.»

### 7.4 Eliminar

Diálogo de confirmación (`modals.openConfirmModal`): «¿Eliminar a [nombre]?», texto «Se borrarán también su programación, reasignaciones y recorridos finalizados. No se puede deshacer.», botones **Cancelar** y **Eliminar** (rojo). Si `enRecorrido`, el botón de eliminar de la tabla está deshabilitado con tooltip «Tiene un recorrido activo».

### 7.5 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-06.1 | Datos completos | Registra | El conductor aparece en la lista como «Disponible» |
| CA-06.2 | Identificación o correo ya registrados | Registra | El mensaje del backend aparece bajo el campo repetido |
| CA-06.3 | Un campo vacío | Toca Registrar | Mensaje bajo el campo, sin llamar a la API |
| CA-07.1 | Conductor existente | Edita y guarda | La lista muestra los cambios |
| CA-07.2 | Conductor sin recorrido | Elimina y confirma | Desaparece de la lista |
| CA-07.3 | Conductor en recorrido | Mira la tabla | Eliminar deshabilitado; si aun así el backend responde 409, se muestra su `mensaje` |
| CA-08.1 | Conductor disponible | Lo marca no disponible con motivo | La tabla muestra «No disponible» y el motivo |
| CA-08.2 | — | Marca no disponible sin motivo | «Seleccione el motivo» bajo el selector |
| CA-08.3 | Conductor no disponible | Lo marca disponible | La tabla muestra «Disponible» |
| CA-08.4 | Conductor en recorrido | Abre el modal | «No disponible» deshabilitado |
| — | Motivo «Otro» sin detalle | Guarda | «Describa el motivo» bajo el detalle |

## 8. Vehículos (HU-28, HU-29, HU-30) — sin mockup, mismo patrón que conductores

**HU-28** — Como administrador de la empresa, quiero registrar un vehículo de la empresa con su placa y su número interno, para saber qué buses pueden salir a operar e identificarlos en los recorridos y en el monitoreo.
**HU-29** — Como administrador de la empresa, quiero modificar los datos de un vehículo o eliminarlo, para mantener actualizado el registro de buses de la empresa.
**HU-30** — Como administrador de la empresa, quiero desactivar un vehículo escribiendo el motivo y reactivarlo cuando vuelva a estar en servicio, para que no se use un bus que no puede operar y quede constancia de por qué se retiró.

### 8.1 Lista (`/vehiculos`)

- Encabezado: «Vehículos», subtítulo «Buses de la empresa habilitados para operar», botón **Registrar vehículo**.
- Filtros: buscador «Placa o número interno» y selector **Todos / Activos / Inactivos** (`activo`).
- Tabla: **Número interno** (en negrita, p. ej. UM-104), **Placa**, **Estado** (chip verde «Activo» o chip gris «Inactivo» con el motivo y «desde [fecha]» debajo), **En recorrido** (chip azul «En recorrido · R1» o «—»), **Acciones**: Editar, Desactivar / Reactivar (según estado), Eliminar.

### 8.2 Registrar / editar

`Drawer` como el de conductores con **Número interno** (obligatorio, máx. 20, ayuda «Por ejemplo UM-104») y **Placa** (obligatoria, máx. 10, ayuda «Se guarda en mayúsculas, sin espacios ni guiones»). Tras guardar, mostrar los valores que devuelve el backend (ya normalizados).

### 8.3 Desactivar / reactivar

- **Desactivar**: modal «Desactivar [UM-104]» con textarea **Motivo** obligatorio (máx. 300, ejemplo de ayuda «Mantenimiento de frenos») y texto «Un vehículo inactivo no puede usarse en recorridos.» Botón **Desactivar**.
- **Reactivar**: confirmación simple «¿Reactivar [UM-104]? Volverá a estar disponible para recorridos.»
- Si `enRecorrido`, los botones Desactivar y Eliminar están deshabilitados con tooltip «Tiene un recorrido activo».

### 8.4 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-28.1 | Datos nuevos | Registra | Aparece como «Activo» con los valores normalizados |
| CA-28.2 | Placa o número interno repetidos | Registra | Mensaje del backend bajo el campo |
| CA-28.3 | Campo vacío | Toca Registrar | Mensaje bajo el campo, sin llamar a la API |
| CA-29.1 | Vehículo existente | Edita y guarda | La lista muestra los cambios |
| CA-29.2 | Vehículo sin recorrido | Elimina y confirma | Desaparece |
| CA-29.3 | Vehículo en recorrido | Mira la tabla | Eliminar deshabilitado; un 409 se muestra si ocurre |
| CA-29.4 | Diálogo de confirmación | Cancela | No se elimina |
| CA-30.1 | Vehículo activo | Desactiva con motivo | Chip «Inactivo» con motivo y fecha |
| CA-30.2 | — | Desactiva sin motivo | «Escriba el motivo de la desactivación» |
| CA-30.3 | Vehículo en recorrido | Mira la tabla | Desactivar deshabilitado |
| CA-30.4 | Vehículo inactivo | Reactiva | Chip «Activo», sin motivo |

## 9. Fechas

`utils/fechas.ts`:

- `formatearFechaHora(iso)` → «24 nov 2026, 10:41 a. m.» en `America/Bogota`.
- `formatearFecha(yyyyMmDd)` → «martes 24 de noviembre».
- `hoyBogota()` → `YYYY-MM-DD` de hoy en `America/Bogota` (se usa en la Fase 3).

## 10. Accesibilidad y calidad mínimas

- Todos los inputs con `label` visible; botones de solo ícono con `aria-label`.
- Estados distinguibles sin depender solo del color (texto en cada chip).
- Foco visible; los modales y el drawer devuelven el foco al cerrarse (Mantine lo hace).
- Funciona en un ancho de 360 px sin que se corte nada (tablas con scroll horizontal).

## 11. Definición de terminado (Fase 1)

- [ ] `npm run dev` arranca y, con el backend en perfil `demo`, se puede iniciar sesión con `admin@urbanmovil.co` / `Admin12345`.
- [ ] `npm run build` termina sin errores de TypeScript y `npm run lint` sin errores.
- [ ] Sesión: restauración al recargar, redirección al login sin sesión, sesión vencida y cierre de sesión funcionan.
- [ ] Conductores y vehículos cumplen todos los criterios de §7.5 y §8.4 probados a mano contra el backend.
- [ ] Todos los errores del backend se muestran con su `mensaje` (por campo o en notificación).
- [ ] `README.md`: requisitos (Node LTS), cómo arrancar junto al backend, credenciales demo, estructura de carpetas y una lista de verificación manual con los CA de esta fase.

## 12. Trazabilidad

| Pantalla | HU | Mockup |
| --- | --- | --- |
| Login | HU-01 | 01 |
| Conductores, registrar/editar | HU-06, HU-07 | 04, 05 |
| Disponibilidad | HU-08 | 06 |
| Vehículos | HU-28, HU-29, HU-30 | — (patrón de 04/05) |
