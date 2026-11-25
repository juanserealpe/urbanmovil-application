# UrbanMóvil API: contrato para el frontend (panel del administrador)

Este documento describe todo lo que el frontend necesita para consumir la API: cómo autenticarse, el formato de errores, los tipos de datos y cada endpoint con sus reglas. Está tomado del código del backend (controllers, DTOs y servicios); si algo no coincide, manda el código y Swagger.

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs` (sirve para generar tipos o un cliente)

---

## 1. Configuración base

| Dato | Valor |
| --- | --- |
| Base URL | `http://localhost:8080/api/v1` |
| Origen del frontend permitido por CORS | `http://localhost:5173` (Vite por defecto). Se cambia con la variable `CORS_ORIGENES` del backend |
| Formato | JSON (`Content-Type: application/json`) en peticiones y respuestas |
| Zona horaria del negocio | `America/Bogota` (UTC-5). «Hoy» siempre lo calcula el backend en esa zona |

Para levantar el backend con datos de prueba:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=demo'
```

Credenciales del administrador inicial: `admin@urbanmovil.co` / `Admin12345`.

### 1.1 Autenticación: sesión con cookie (NO hay JWT)

- `POST /auth/login` crea una sesión en el servidor y devuelve la cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`, vence a las 8 h).
- El frontend **no puede leer** la cookie (es `HttpOnly`) ni debe guardar ningún token. El navegador la envía sola.
- **Todas** las peticiones deben ir con credenciales:
  - `fetch`: `credentials: 'include'`
  - axios: `withCredentials: true`
- Para saber si hay sesión al cargar la app, llamar a `GET /auth/me`: `200` = sesión activa, `401` = mostrar login.
- Cualquier `401` con `codigo: "NO_AUTENTICADO"` en cualquier endpoint significa que la sesión venció o no existe. Hay que limpiar el estado del usuario y redirigir al login.
- La sesión vive en memoria del servidor: si el backend se reinicia, todas las sesiones se pierden.
- CSRF está deshabilitado en `/api/**`. No hace falta enviar ningún token CSRF.
- Solo los administradores pueden iniciar sesión. Los conductores NO entran al panel.

Alternativa recomendada en desarrollo: un proxy de Vite (`/api` → `http://localhost:8080`), así frontend y API comparten origen y se evitan los problemas de CORS:

```ts
// vite.config.ts
export default defineConfig({
  server: { proxy: { '/api': 'http://localhost:8080' } },
});
// y la base URL del cliente pasa a ser '/api/v1'
```

### 1.2 Endpoints públicos (sin sesión)

- `POST /auth/login`
- `GET /public/**` (rutas para la app del pasajero)

Todo lo demás bajo `/api/v1/**` exige sesión de administrador.

---

## 2. Formato de errores (único para toda la API)

Todas las respuestas de error tienen este cuerpo:

```ts
interface ErrorResponse {
  codigo: CodigoError;
  mensaje: string;      // listo para mostrar al usuario, en español
  campos: CampoError[]; // vacío si el error no es de un campo concreto
}

interface CampoError {
  campo: string;   // nombre del campo del request: "correo", "placa", "alternos[1].puntos", "rutas"...
  mensaje: string; // mensaje para mostrar debajo de ese campo
}

type CodigoError =
  | 'VALIDACION'             // 400
  | 'NO_AUTENTICADO'         // 401: sin sesión o sesión vencida → ir al login
  | 'CREDENCIALES_INVALIDAS' // 401: solo en el login
  | 'ACCESO_DENEGADO'        // 403
  | 'NO_ENCONTRADO'          // 404
  | 'DATO_REPETIDO'          // 409: valores únicos ya registrados (vienen en `campos`)
  | 'REGLA_NEGOCIO'          // 409: una regla impide la acción (mostrar `mensaje`, p. ej. en un toast)
  | 'METODO_NO_PERMITIDO'    // 405
  | 'ERROR_INTERNO';         // 500
```

Cómo manejarlo en la UI:

1. **`VALIDACION` y `DATO_REPETIDO`**: recorrer `campos` y mostrar cada `mensaje` debajo del input cuyo nombre coincide con `campo`. Si `campos` está vacío, mostrar `mensaje` como error general del formulario.
2. **`REGLA_NEGOCIO`**: mostrar `mensaje` en un aviso (toast o alerta). No es un error de un campo.
3. **`NO_AUTENTICADO`**: redirigir al login (excepto si ya estás en el login).
4. **`ERROR_INTERNO`**: mensaje genérico y opción de reintentar.

En las rutas, los errores de trazado usan nombres de campo con índice: `recorridoPrincipal`, `alternos`, `alternos[0].puntos`, `alternos[2].id`, `alternos[1].nombre`.

El backend **recorta los espacios** de todos los textos antes de validar. Un texto con solo espacios cuenta como vacío.

---

## 3. Convenciones de datos

| Tipo | Formato | Ejemplo |
| --- | --- | --- |
| Ids | `number` (entero) | `3` |
| Fecha (`LocalDate`) | `YYYY-MM-DD` | `"2026-11-24"` |
| Fecha y hora (`OffsetDateTime`) | ISO-8601 con offset de Bogotá | `"2026-11-24T10:00:00-05:00"` |
| Color | `#RRGGBB` (el backend lo guarda en mayúsculas) | `"#0057B8"` |
| Punto | `{ lat, lng }` en grados decimales | `{ "lat": 2.4419, "lng": -76.6066 }` |

Las listas **no están paginadas**: los endpoints `GET` de listado devuelven arrays completos.

---

## 4. Tipos TypeScript

```ts
// ---------- Comunes ----------
export interface Punto { lat: number; lng: number }

export interface EnRecorrido {
  recorridoId: number;
  rutaCodigo: string;
}

// ---------- Auth ----------
export interface LoginRequest { correo: string; contrasena: string }
export interface Administrador { id: number; nombre: string; correo: string }

// ---------- Conductores ----------
export type MotivoNoDisponible = 'INCAPACIDAD' | 'VACACIONES' | 'PERMISO' | 'OTRO';

export interface Conductor {
  id: number;
  nombre: string;
  identificacion: string;
  correo: string;
  telefono: string;
  disponible: boolean;
  motivoNoDisponible: MotivoNoDisponible | null;
  detalleMotivo: string | null;
  enRecorrido: EnRecorrido | null; // null si no está recorriendo ninguna ruta
  rutasHoy: string[];              // códigos de las rutas de su programación de hoy, en orden
}

export interface ConductorCrearRequest {
  nombre: string;         // obligatorio, máx 120
  identificacion: string; // obligatorio, máx 20, único
  correo: string;         // obligatorio, correo válido, máx 120, único (se guarda en minúsculas)
  telefono: string;       // obligatorio, máx 20
  contrasena: string;     // obligatoria, mínimo 8
}

export interface ConductorModificarRequest {
  nombre: string;
  identificacion: string;
  correo: string;
  telefono: string;
  contrasena?: string | null; // vacía, null o ausente = no se cambia; si llega, mínimo 8
}

export interface DisponibilidadRequest {
  disponible: boolean;                 // obligatorio
  motivo?: MotivoNoDisponible | null;  // obligatorio si disponible = false
  detalle?: string | null;             // máx 300; obligatorio si motivo = 'OTRO'
}

// ---------- Vehículos ----------
export interface Vehiculo {
  id: number;
  placa: string;                     // normalizada: mayúsculas, sin espacios ni guiones ("ABC123")
  numeroInterno: string;             // en mayúsculas ("UM-104")
  activo: boolean;
  motivoInactivacion: string | null;
  fechaInactivacion: string | null;  // OffsetDateTime
  enRecorrido: EnRecorrido | null;
}

export interface VehiculoRequest {   // mismo cuerpo para crear (POST) y modificar (PUT)
  placa: string;         // obligatoria, máx 10, única
  numeroInterno: string; // obligatorio, máx 20, único
}

export interface EstadoVehiculoRequest {
  activo: boolean;         // obligatorio
  motivo?: string | null;  // máx 300; obligatorio si activo = false
}

// ---------- Rutas ----------
export interface CaminoAlterno { id: number; nombre: string; puntos: Punto[] }

export interface RutaResumen {
  id: number;
  codigo: string;
  nombre: string;
  color: string;
  puntosRecorrido: number;   // cantidad de puntos del recorrido principal
  numAlternos: number;
  recorridosActivos: number; // buses recorriéndola ahora mismo
  conductoresHoy: string[];  // nombres de los conductores que la tienen hoy en su programación
  actualizadoEn: string;     // OffsetDateTime
}

export interface RutaDetalle extends RutaResumen {
  recorridoPrincipal: Punto[]; // en el sentido de circulación
  inicio: Punto | null;        // primer punto del recorrido principal
  final: Punto | null;         // último punto del recorrido principal
  alternos: CaminoAlterno[];
}

export interface CaminoAlternoRequest {
  id?: number | null; // solo en PUT: con id se actualiza ese alterno; sin id se crea uno nuevo
  nombre: string;     // obligatorio, máx 120
  puntos: Punto[];    // mínimo 2
}

export interface RutaRequest {  // mismo cuerpo para crear (POST) y modificar (PUT)
  codigo: string;               // obligatorio, máx 10, único (se guarda en mayúsculas)
  nombre: string;               // obligatorio, máx 120, único
  color?: string | null;        // opcional, "#RRGGBB"
  recorridoPrincipal: Punto[];  // mínimo 2, máximo 2000
  alternos?: CaminoAlternoRequest[]; // máximo 10
}

export interface RutaPublica {
  codigo: string;
  nombre: string;
  color: string;
  recorridoPrincipal: Punto[];
  alternos: { nombre: string; puntos: Punto[] }[];
}

// ---------- Programación ----------
export type EstadoRutaProgramada = 'PENDIENTE' | 'EN_RECORRIDO' | 'RETIRO_PENDIENTE';

export interface RutaProgramada {
  rutaId: number;
  codigo: string;
  nombre: string;
  color: string;
  orden: number; // 1, 2, 3...
  estado: EstadoRutaProgramada;
}

export interface Programacion {
  conductorId: number;
  fecha: string; // YYYY-MM-DD
  rutas: RutaProgramada[]; // ordenadas por `orden`
}

export interface ProgramacionRequest {
  rutas: number[]; // lista COMPLETA y ORDENADA de ids de ruta ([] deja la programación vacía)
}

export interface ProgramacionResumen {
  conductorId: number;
  nombre: string;
  disponible: boolean;
  enRecorrido: EnRecorrido | null;
  totalRutas: number;
}

// ---------- Reasignaciones ----------
export interface PersonaResumen { id: number; nombre: string }

export interface ReasignacionRequest {
  motivo: string;          // obligatorio, máx 300
  rutasAsignar?: number[]; // ids que se agregan AL FINAL de la programación de hoy
  rutasQuitar?: number[];  // ids que se quitan de la programación de hoy
  // al menos una de las dos listas debe tener elementos
}

export interface Reasignacion {
  id: number;
  fechaHora: string; // OffsetDateTime
  conductor: PersonaResumen;
  administrador: PersonaResumen; // el administrador que hizo la reasignación
  motivo: string;
  rutasAsignadas: string[]; // códigos de ruta
  rutasQuitadas: string[];  // códigos de ruta
  programacion: Programacion | null; // solo viene en la respuesta del POST
}

// ---------- Monitoreo ----------
export interface BusMonitoreo {
  recorridoId: number;
  vehiculo: { id: number; numeroInterno: string; placa: string };
  ruta: { id: number; codigo: string; nombre: string; color: string };
  conductor: { id: number; nombre: string };
  posicion: Punto;      // posición SIMULADA
  horaPosicion: string; // OffsetDateTime
}

export interface Monitoreo {
  simulado: boolean;   // siempre true en este MVP (no hay GPS)
  generadoEn: string;  // OffsetDateTime
  buses: BusMonitoreo[];
}

// ---------- Demo (solo perfil demo) ----------
export type EstadoRecorrido = 'ACTIVO' | 'FINALIZADO';

export interface IniciarRecorridoRequest { conductorId: number; rutaId: number; vehiculoId: number }

export interface RecorridoDemo {
  id: number;
  conductor: { id: number; nombre: string };
  ruta: { id: number; codigo: string; nombre: string };
  vehiculo: { id: number; numeroInterno: string; placa: string };
  estado: EstadoRecorrido;
  inicio: string;      // OffsetDateTime
  fin: string | null;  // OffsetDateTime
}
```

---

## 5. Endpoints

Todas las rutas son relativas a la base URL `/api/v1`. 🔒 = requiere sesión de administrador.

### Resumen

| Método | Ruta | Auth | Respuesta |
| --- | --- | --- | --- |
| POST | `/auth/login` | Pública | `200 Administrador` |
| POST | `/auth/logout` | 🔒 | `204` |
| GET | `/auth/me` | 🔒 | `200 Administrador` |
| GET | `/conductores?q=&disponible=` | 🔒 | `200 Conductor[]` |
| GET | `/conductores/{id}` | 🔒 | `200 Conductor` |
| POST | `/conductores` | 🔒 | `201 Conductor` |
| PUT | `/conductores/{id}` | 🔒 | `200 Conductor` |
| DELETE | `/conductores/{id}` | 🔒 | `204` |
| PATCH | `/conductores/{id}/disponibilidad` | 🔒 | `200 Conductor` |
| GET | `/conductores/{id}/programacion?fecha=` | 🔒 | `200 Programacion` |
| PUT | `/conductores/{id}/programacion?fecha=` | 🔒 | `200 Programacion` |
| POST | `/conductores/{id}/reasignaciones` | 🔒 | `201 Reasignacion` |
| GET | `/programaciones?fecha=` | 🔒 | `200 ProgramacionResumen[]` |
| GET | `/reasignaciones?conductorId=&fecha=` | 🔒 | `200 Reasignacion[]` |
| GET | `/vehiculos?q=&activo=` | 🔒 | `200 Vehiculo[]` |
| GET | `/vehiculos/{id}` | 🔒 | `200 Vehiculo` |
| POST | `/vehiculos` | 🔒 | `201 Vehiculo` |
| PUT | `/vehiculos/{id}` | 🔒 | `200 Vehiculo` |
| DELETE | `/vehiculos/{id}` | 🔒 | `204` |
| PATCH | `/vehiculos/{id}/estado` | 🔒 | `200 Vehiculo` |
| GET | `/rutas?q=` | 🔒 | `200 RutaResumen[]` |
| GET | `/rutas/{id}` | 🔒 | `200 RutaDetalle` |
| POST | `/rutas` | 🔒 | `201 RutaDetalle` |
| PUT | `/rutas/{id}` | 🔒 | `200 RutaDetalle` |
| DELETE | `/rutas/{id}` | 🔒 | `204` |
| GET | `/public/rutas` | Pública | `200 RutaPublica[]` |
| GET | `/monitoreo/buses` | 🔒 | `200 Monitoreo` |
| GET | `/demo/recorridos?estado=` | 🔒 solo perfil demo | `200 RecorridoDemo[]` |
| POST | `/demo/recorridos` | 🔒 solo perfil demo | `201 RecorridoDemo` |
| POST | `/demo/recorridos/{id}/finalizar` | 🔒 solo perfil demo | `200 RecorridoDemo` |

---

### 5.1 Autenticación (HU-01)

#### `POST /auth/login` (pública)

```json
{ "correo": "admin@urbanmovil.co", "contrasena": "Admin12345" }
```

- `200` → `Administrador` y la cookie `JSESSIONID` queda guardada en el navegador.
- `400 VALIDACION` → `campos`: `correo` («Escriba el correo») o `contrasena` («Escriba la contraseña»).
- `401 CREDENCIALES_INVALIDAS` → «Credenciales inválidas». Es el mismo mensaje si el correo no existe o si la contraseña está mal (no revelar cuál fue).

#### `POST /auth/logout` 🔒

Sin cuerpo. `204`. Después, limpiar el estado del usuario y volver al login.

#### `GET /auth/me` 🔒

`200 Administrador` si hay sesión; `401 NO_AUTENTICADO` si no. Usarlo al arrancar la app para restaurar la sesión.

---

### 5.2 Conductores (HU-06, HU-07, HU-08)

#### `GET /conductores` 🔒

Query opcional:
- `q`: busca (sin distinguir mayúsculas) en nombre, identificación o correo.
- `disponible`: `true` | `false`.

Devuelve `Conductor[]` ordenado por nombre.

#### `GET /conductores/{id}` 🔒

`200 Conductor` · `404` «El conductor no existe».

#### `POST /conductores` 🔒

```json
{
  "nombre": "Andrés Felipe Muñoz Ruiz",
  "identificacion": "1061000001",
  "correo": "andres@correo.co",
  "telefono": "3001234567",
  "contrasena": "Conductor123"
}
```

- `201 Conductor` (nace `disponible: true`).
- `400 VALIDACION` → mensajes por campo: «Escriba el nombre», «Escriba un correo válido», «La contraseña debe tener al menos 8 caracteres», etc.
- `409 DATO_REPETIDO` → `campos` puede traer `identificacion` («Ya existe un conductor con este número de identificación») y/o `correo` («Ya existe un conductor con este correo»).

#### `PUT /conductores/{id}` 🔒

Mismo cuerpo que el POST, pero `contrasena` es opcional: vacía o ausente = no se cambia. **No cambia la disponibilidad** (para eso está el PATCH).

`200 Conductor` · `400` · `404` · `409 DATO_REPETIDO`.

#### `DELETE /conductores/{id}` 🔒

Elimina definitivamente el conductor y su historial.

- `204`.
- `404` «El conductor no existe».
- `409 REGLA_NEGOCIO` «No se puede eliminar: el conductor tiene un recorrido activo. Debe finalizarlo primero.»

UI: pedir confirmación antes de eliminar. Si `enRecorrido != null`, conviene deshabilitar el botón y explicar por qué.

#### `PATCH /conductores/{id}/disponibilidad` 🔒

```json
{ "disponible": false, "motivo": "OTRO", "detalle": "Cita médica" }
```
```json
{ "disponible": true }
```

- Con `disponible: true`, `motivo` y `detalle` se ignoran y se borran.
- Con `disponible: false`:
  - `motivo` obligatorio → si falta, `400` en `motivo`: «Seleccione el motivo».
  - Si `motivo = OTRO`, `detalle` obligatorio → `400` en `detalle`: «Describa el motivo».
  - `409 REGLA_NEGOCIO` «No se puede marcar como no disponible mientras tiene un recorrido activo.»
- `404` si el conductor no existe.

UI: un select con los motivos `INCAPACIDAD`, `VACACIONES`, `PERMISO` y `OTRO`. El textarea `detalle` es opcional, y pasa a ser obligatorio con `OTRO`.

---

### 5.3 Vehículos (HU-28, HU-29, HU-30)

#### `GET /vehiculos` 🔒

Query opcional:
- `q`: busca en placa (también normalizada, así `abc-1` encuentra `ABC123`) o número interno.
- `activo`: `true` | `false`.

Devuelve `Vehiculo[]` ordenado por número interno.

#### `GET /vehiculos/{id}` 🔒

`200 Vehiculo` · `404` «El vehículo no existe».

#### `POST /vehiculos` 🔒 · `PUT /vehiculos/{id}` 🔒

```json
{ "placa": "abc-123", "numeroInterno": "um-104" }
```

- La placa se guarda en mayúsculas, sin espacios ni guiones (`ABC123`). El número interno, en mayúsculas (`UM-104`). Mostrar siempre lo que devuelve la respuesta.
- `201` (POST) / `200` (PUT) → `Vehiculo`. Un vehículo nuevo nace `activo: true`.
- `400 VALIDACION` → «Escriba la placa», «Escriba el número interno», máximos de longitud.
- `409 DATO_REPETIDO` → `placa` («Ya existe un vehículo con esta placa») y/o `numeroInterno` («Ya existe un vehículo con este número interno»).
- `404` en PUT si no existe.

#### `DELETE /vehiculos/{id}` 🔒

`204` · `404` · `409 REGLA_NEGOCIO` «No se puede eliminar: el vehículo tiene un recorrido activo. Debe finalizarse primero.»

#### `PATCH /vehiculos/{id}/estado` 🔒

```json
{ "activo": false, "motivo": "Mantenimiento de frenos" }
```
```json
{ "activo": true }
```

- Desactivar: `motivo` obligatorio → `400` en `motivo`: «Escriba el motivo de la desactivación».
- Desactivar con recorrido activo → `409 REGLA_NEGOCIO` «No se puede desactivar mientras tiene un recorrido activo.»
- Reactivar borra `motivoInactivacion` y `fechaInactivacion`.
- Si ya estaba inactivo y se desactiva de nuevo, solo se actualiza el motivo y se conserva la fecha original.

---

### 5.4 Rutas (HU-02, HU-03, HU-04, HU-05)

Una ruta tiene un **recorrido principal** (una polilínea en el sentido de circulación: el primer punto es el inicio y el último el final) y hasta 10 **caminos alternos**. Los alternos solo se muestran en el mapa; el backend nunca los usa para calcular nada.

#### `GET /rutas` 🔒

Query opcional `q`: busca en código o nombre. Devuelve `RutaResumen[]` ordenado por código (sin los puntos, para listados).

#### `GET /rutas/{id}` 🔒

`200 RutaDetalle` (con `recorridoPrincipal`, `inicio`, `final` y `alternos` con sus `id`) · `404` «La ruta no existe».

#### `POST /rutas` 🔒

```json
{
  "codigo": "R9",
  "nombre": "Centro – Hospital San José",
  "color": "#0057B8",
  "recorridoPrincipal": [
    { "lat": 2.4419, "lng": -76.6066 },
    { "lat": 2.4436, "lng": -76.6049 },
    { "lat": 2.4462, "lng": -76.6031 }
  ],
  "alternos": [
    {
      "nombre": "Desvío por Cra 9",
      "puntos": [
        { "lat": 2.4419, "lng": -76.6066 },
        { "lat": 2.4440, "lng": -76.6085 },
        { "lat": 2.4462, "lng": -76.6031 }
      ]
    }
  ]
}
```

- `color` es opcional. Si no llega (o llega vacío), el backend asigna uno de esta paleta, en orden de creación: `#0057B8`, `#D62828`, `#2A9D8F`, `#F4A261`, `#6A4C93`, `#E76F51`.
- `codigo` se guarda en mayúsculas.
- `201 RutaDetalle`.

Errores de validación (`400 VALIDACION`, con el `campo` indicado):

| Campo | Mensaje | Cuándo |
| --- | --- | --- |
| `codigo` | «Escriba el código de la ruta» / «El código admite máximo 10 caracteres» | |
| `nombre` | «Escriba el nombre de la ruta» / «El nombre admite máximo 120 caracteres» | |
| `color` | «Color inválido» | No cumple `#RRGGBB` |
| `recorridoPrincipal` | «Falta trazar el recorrido principal» | Ausente o vacío |
| `recorridoPrincipal` | «La ruta debe tener punto de inicio y punto final» | Solo 1 punto |
| `recorridoPrincipal` / `alternos[i].puntos` | «Coordenadas inválidas» | `lat` fuera de [-90, 90], `lng` fuera de [-180, 180] o nulos |
| `recorridoPrincipal` / `alternos[i].puntos` | «El trazado supera el máximo de puntos permitido» | Más de 2000 puntos |
| `alternos` | «Una ruta admite máximo 10 caminos alternos» | |
| `alternos[i].nombre` | «Escriba el nombre del camino alterno» | |
| `alternos[i].puntos` | «Cada camino alterno debe tener al menos dos puntos» | |

`409 DATO_REPETIDO` → `codigo` («Ya existe una ruta con este código») y/o `nombre` («Ya existe una ruta con este nombre»).

#### `PUT /rutas/{id}` 🔒 (reemplazo completo)

Mismo cuerpo que el POST, y **reemplaza todo**:
- Alterno **con `id`** → se actualiza ese alterno.
- Alterno **sin `id`** → se crea uno nuevo.
- Alterno que **no viene** en la lista → se elimina.
- Si `color` no llega, se conserva el color actual.

Por eso el formulario de edición debe partir de `GET /rutas/{id}` y enviar SIEMPRE la lista completa de alternos, con sus `id`.

Errores adicionales:
- `400` en `alternos[i].id`: «El camino alterno no pertenece a esta ruta».
- `400` en `alternos`: «Un camino alterno viene repetido» (el mismo `id` dos veces).
- `404` si la ruta no existe.

Si algo es inválido, **no se guarda nada**.

#### `DELETE /rutas/{id}` 🔒

`204` · `404` · `409 REGLA_NEGOCIO` «No se puede eliminar: la ruta tiene recorridos activos.» Al eliminarla también salen de las programaciones y del historial de reasignaciones.

#### `GET /public/rutas` (pública)

`RutaPublica[]` ordenado por código, sin ids. Es lo que consume la app del pasajero; el panel normalmente no lo necesita.

---

### 5.5 Programación del día (HU-09)

La programación es la **lista ordenada de rutas** que hará un conductor en una fecha. Por defecto, «hoy» en Bogotá.

#### `GET /conductores/{id}/programacion?fecha=YYYY-MM-DD` 🔒

`fecha` es opcional (por defecto hoy). Si no hay programación, devuelve `rutas: []`.

Cada ruta trae un `estado`:
- `PENDIENTE`: está programada.
- `EN_RECORRIDO`: el conductor la está recorriendo ahora (solo aplica a hoy).
- `RETIRO_PENDIENTE`: se quitó mientras la recorría; sale sola de la programación cuando termine el recorrido.

`404` si el conductor no existe.

#### `PUT /conductores/{id}/programacion?fecha=YYYY-MM-DD` 🔒

```json
{ "rutas": [3, 1, 2] }
```

`rutas` es la lista **COMPLETA y ORDENADA** de ids: el orden del array define el `orden` (1, 2, 3...). Las rutas que no vienen se quitan. `[]` deja la programación vacía.

- `200 Programacion`.
- `400` en `rutas`: «Indique las rutas de la programación» (si `rutas` es null) o «La ruta R1 está repetida en la programación».
- `404` «El conductor no existe» / «No existe la ruta con id X».
- `409 REGLA_NEGOCIO`:
  - «No se puede modificar la programación de un día pasado».
  - «No se puede retirar la ruta R1: el conductor la está recorriendo. Se podrá retirar cuando termine.»

UI recomendada: una lista de rutas que se puede reordenar arrastrando (drag & drop), con un botón para agregar rutas desde `GET /rutas`. Bloquear el botón de quitar en la ruta `EN_RECORRIDO`. Al guardar, mandar el array de ids en el orden visible.

#### `GET /programaciones?fecha=YYYY-MM-DD` 🔒

Resumen del día de **todos** los conductores (ordenado por nombre): `ProgramacionResumen[]`. Sirve para la pantalla principal de programación, y al hacer clic en un conductor se abre su detalle con el GET anterior.

---

### 5.6 Reasignaciones por eventualidad (HU-25)

Una reasignación cambia **la programación de HOY** de un conductor por una eventualidad, y deja constancia (historial con motivo y administrador).

#### `POST /conductores/{id}/reasignaciones` 🔒

```json
{
  "motivo": "Diana reportó falla mecánica",
  "rutasAsignar": [2],
  "rutasQuitar": [1]
}
```

- Las rutas a asignar se agregan **al final**, en el orden recibido.
- Si se quita la ruta que el conductor está recorriendo, no se borra: queda en `RETIRO_PENDIENTE` hasta que termine.
- El administrador se toma de la sesión, no se envía.
- `201 Reasignacion`, que incluye `programacion` con el estado resultante (sirve para refrescar la vista sin otro GET).

Errores:
- `400` en `motivo`: «Escriba qué pasó».
- `400` en `rutasAsignar`: «Indique al menos una ruta para asignar o quitar».
- `400` en `rutasAsignar` / `rutasQuitar`: «La ruta R2 está repetida».
- `400` en `rutasQuitar`: «La ruta R2 no puede asignarse y quitarse a la vez».
- `404` conductor o ruta inexistente.
- `409 REGLA_NEGOCIO`: «La ruta R2 ya está en la programación» / «La ruta R1 no está en la programación».

UI: un modal desde el detalle del conductor, con un textarea para el motivo, un selector múltiple de rutas a asignar (las que NO tiene hoy) y otro de rutas a quitar (las que SÍ tiene hoy). Filtrar así las opciones evita la mayoría de los `409`.

#### `GET /reasignaciones?conductorId=&fecha=YYYY-MM-DD` 🔒

Historial, **el más reciente primero**. Los dos filtros son opcionales; `fecha` filtra por el día de `fechaHora` en Bogotá. Aquí `programacion` viene `null`.

---

### 5.7 Monitoreo de buses (HU-31)

#### `GET /monitoreo/buses` 🔒

Devuelve un elemento por cada recorrido activo, con la posición del bus sobre el recorrido principal de su ruta.

- **La posición es SIMULADA** (`simulado: true` siempre): el backend la calcula a partir de la hora, a 18 km/h constantes y dando vueltas a la ruta. No hay GPS. La UI debe indicarlo (p. ej. con una etiqueta «Posición simulada»).
- **Polling cada 3 a 5 segundos.** Detenerlo cuando la pestaña o la pantalla no están visibles (`document.visibilityState`).
- Dibujar cada bus con el `color` de su ruta. Si además se quiere pintar el trazado, se trae de `GET /rutas/{id}`, y conviene cachearlo porque no cambia entre consultas.
- Cuando no hay recorridos activos, `buses` viene vacío. Mostrar un estado vacío del tipo «No hay buses en recorrido».

Para el mapa se recomienda Leaflet (`react-leaflet`) con tiles de OpenStreetMap. Las rutas de demo están en Popayán (~ `2.444, -76.606`).

---

### 5.8 Recorridos de demostración (solo con el perfil `demo`)

La app del conductor todavía no existe, así que los recorridos se inician y finalizan desde aquí. **Sin el perfil `demo`, todo `/demo/**` responde `404`** («Los recorridos de demostración solo existen con el perfil demo»). El frontend debería mostrar esta sección solo si `GET /demo/recorridos` no devuelve 404.

#### `GET /demo/recorridos?estado=ACTIVO|FINALIZADO` 🔒

`RecorridoDemo[]`. `estado` es opcional.

#### `POST /demo/recorridos` 🔒

```json
{ "conductorId": 3, "rutaId": 1, "vehiculoId": 2 }
```

`201 RecorridoDemo` con `estado: ACTIVO` e `inicio = ahora`.

Errores:
- `400` en `conductorId` / `rutaId` / `vehiculoId`: «Indique el conductor», «Indique la ruta», «Indique el vehículo».
- `404` conductor, ruta o vehículo inexistente.
- `409 REGLA_NEGOCIO`:
  - «El conductor no está disponible»
  - «La ruta no está en la programación de hoy del conductor»
  - «El vehículo está inactivo y no puede usarse en recorridos»
  - «El conductor ya tiene un recorrido activo»
  - «El vehículo ya está en otro recorrido activo»

#### `POST /demo/recorridos/{id}/finalizar` 🔒

Sin cuerpo. `200 RecorridoDemo` con `estado: FINALIZADO` y `fin`. Si la ruta estaba en `RETIRO_PENDIENTE`, sale de la programación en ese momento.

`404` «El recorrido no existe» · `409` «El recorrido ya está finalizado».

---

## 6. Reglas de negocio que la UI debe reflejar

| Regla | Dónde se nota |
| --- | --- |
| Un conductor con `enRecorrido` no se puede eliminar ni marcar como no disponible | Deshabilitar esas acciones y mostrar «En recorrido: R1» |
| Un vehículo con `enRecorrido` no se puede eliminar ni desactivar | Ídem |
| Una ruta con `recorridosActivos > 0` no se puede eliminar | Ídem |
| La ruta `EN_RECORRIDO` no se puede quitar de la programación de hoy | Bloquear el botón de quitar; si se quita por reasignación, queda `RETIRO_PENDIENTE` |
| No se edita la programación de días pasados | Deshabilitar la edición cuando `fecha < hoy` |
| Las reasignaciones solo aplican a la programación de HOY | El modal no pide fecha |
| No disponible exige motivo; `OTRO` exige detalle | Validar en el formulario antes de enviar |
| Desactivar un vehículo exige motivo | Ídem |
| La posición del monitoreo es simulada | Etiqueta visible en el mapa |
| Aunque la UI ya bloquee estas acciones, el backend vuelve a validarlas | Manejar SIEMPRE el `409` mostrando `mensaje` |

---

## 7. Pantallas sugeridas para el panel

1. **Login**: `POST /auth/login`. Errores de campo y «Credenciales inválidas».
2. **Layout autenticado**: `GET /auth/me` al cargar; nombre del administrador y botón «Cerrar sesión» (`POST /auth/logout`).
3. **Conductores**: tabla con búsqueda (`q`) y filtro de disponibilidad, badges de «Disponible / No disponible (motivo)» y «En recorrido», y `rutasHoy`. Formularios de crear/editar, modal de disponibilidad y confirmación para eliminar.
4. **Vehículos**: tabla con búsqueda y filtro activo/inactivo; crear/editar; modal para desactivar con motivo; reactivar; eliminar.
5. **Rutas**: listado (`RutaResumen`). Editor con mapa para trazar el recorrido principal (clics que agregan puntos, en orden) y los alternos; selector de color; vista de detalle.
6. **Programación**: resumen del día (`GET /programaciones`) con selector de fecha; detalle por conductor con lista reordenable y guardado (`PUT`).
7. **Reasignaciones**: modal desde el conductor (`POST`) e historial con filtros por conductor y fecha.
8. **Monitoreo**: mapa con los buses y polling cada 3 a 5 s.
9. **Demo** (solo si existe): iniciar recorridos (selects de conductor, ruta de su programación de hoy y vehículo activo) y finalizarlos.

---

## 8. Cliente HTTP de referencia

```ts
// src/api/client.ts
const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';

export class ApiError extends Error {
  constructor(public status: number, public body: ErrorResponse) {
    super(body.mensaje);
  }
  /** Errores por campo, listos para un formulario: { correo: 'Ya existe...' } */
  get porCampo(): Record<string, string> {
    return Object.fromEntries(this.body.campos.map((c) => [c.campo, c.mensaje]));
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    credentials: 'include', // OBLIGATORIO: envía la cookie JSESSIONID
    headers: { 'Content-Type': 'application/json', ...init.headers },
  });

  if (res.status === 204) return undefined as T;

  const body = await res.json().catch(() => null);
  if (!res.ok) {
    const error: ErrorResponse = body ?? {
      codigo: 'ERROR_INTERNO',
      mensaje: 'Ocurrió un error inesperado. Intente de nuevo.',
      campos: [],
    };
    if (res.status === 401 && error.codigo === 'NO_AUTENTICADO') {
      window.dispatchEvent(new Event('sesion-expirada')); // el router escucha y manda al login
    }
    throw new ApiError(res.status, error);
  }
  return body as T;
}

// Ejemplos
export const login = (datos: LoginRequest) =>
  api<Administrador>('/auth/login', { method: 'POST', body: JSON.stringify(datos) });

export const listarConductores = (q?: string, disponible?: boolean) => {
  const params = new URLSearchParams();
  if (q) params.set('q', q);
  if (disponible !== undefined) params.set('disponible', String(disponible));
  return api<Conductor[]>(`/conductores?${params}`);
};

export const guardarProgramacion = (conductorId: number, rutas: number[], fecha?: string) =>
  api<Programacion>(
    `/conductores/${conductorId}/programacion${fecha ? `?fecha=${fecha}` : ''}`,
    { method: 'PUT', body: JSON.stringify({ rutas }) },
  );
```

---

## 9. Datos del perfil `demo` (base recién creada)

| Tipo | Datos |
| --- | --- |
| Conductores | 1 Andrés (disponible), 2 Laura (disponible), 3 Carlos (disponible), 4 Diana (no disponible, `VACACIONES`). Contraseña de todos: `Conductor123` |
| Vehículos | 1 a 4 activos (`UM-104`, `UM-118`, `UM-221`, `UM-305`); 5 `UM-410` inactivo |
| Rutas | 1 `R1` Centro – Campanario (1 alterno), 2 `R2` Bello Horizonte – Terminal, 3 `R3` La Esmeralda – Pomona (2 alternos) |
| Programación de hoy | Andrés `[R1, R2]`, Laura `[R2]`, Carlos `[R1, R2]` (la R2 se la dio una reasignación) |
| Recorridos activos | 1: Andrés en `R1` con `UM-104`; 2: Laura en `R2` con `UM-305` |

Con solo arrancar, el monitoreo ya muestra 2 buses. La semilla usa la fecha del arranque: si se arranca otro día, la programación queda en el pasado. Para reiniciar: `cd backend && docker compose down -v` y volver a arrancar con el perfil `demo`.
