# UrbanMovil API

Backend de UrbanMovil (MVP académico). Hoy sirve al panel web de administración y expone endpoints públicos para las apps móviles. Esta versión cubre:

- **Fase 1**: proyecto base, esquema completo de base de datos, login del administrador, conductores (con disponibilidad) y vehículos (con estado).
- **Fase 2**: rutas con recorrido principal y caminos alternos, y endpoint público de rutas.
- **Fase 3**: programación del día, reasignaciones con historial, recorridos de demostración y monitoreo de buses con posición simulada.

## Requisitos

- **JDK 21**
- **Docker** encendido (con Docker Compose v2). Lo usan la app, para levantar PostgreSQL, y las pruebas (Testcontainers).

No hace falta instalar PostgreSQL ni Gradle: el proyecto trae `./gradlew`.

## Cómo arrancar

### Normal

```bash
./gradlew bootRun
```

Spring Boot lee `compose.yaml`, levanta PostgreSQL 18 en Docker (base `urbanmovil`, usuario y contraseña `urbanmovil`, puerto `5432`) y configura la conexión solo. Flyway aplica `V1__esquema_inicial.sql`. La base de datos sigue encendida cuando se detiene la app (`lifecycle-management: start-only`), y los datos se guardan en el volumen `urbanmovil-api_urbanmovil-pgdata`. El proyecto de Compose se llama `urbanmovil-api` (está fijo en `compose.yaml`), así que no depende del nombre de la carpeta.

La base de datos arranca vacía, salvo el administrador inicial. Sin el perfil `demo`, los endpoints `/api/v1/demo/...` responden 404.

### Con datos de demostración (perfil `demo`)

```bash
./gradlew bootRun --args='--spring.profiles.active=demo'
```

Si las tablas están vacías, se crean:

- **4 conductores** ficticios. Todos usan la contraseña `Conductor123`.

  | Id | Conductor | Estado |
  | --- | --- | --- |
  | 1 | Andrés Felipe Muñoz Ruiz | Disponible |
  | 2 | Laura Marcela Idrobo Paz | Disponible |
  | 3 | Carlos Eduardo Chicangana López | Disponible |
  | 4 | Diana Patricia Velasco Ordóñez | No disponible por `VACACIONES` |

- **5 vehículos**: `UM-104`, `UM-118`, `UM-221` y `UM-305` activos (ids 1 a 4), y `UM-410` inactivo por «Mantenimiento de frenos» (id 5).
- **3 rutas** sobre Popayán:
  - `R1` Centro – Campanario (id 1), con 1 alterno.
  - `R2` Bello Horizonte – Terminal (id 2), sin alternos.
  - `R3` La Esmeralda – Pomona (id 3), con 2 alternos.
- **Programación de hoy**: Andrés → `[R1, R2]`, Laura → `[R2]` y Carlos → `[R1]`.
- **2 recorridos activos**, iniciados hace unos minutos: Andrés en `R1` con `UM-104` (recorrido 1) y Laura en `R2` con `UM-305` (recorrido 2).
- **1 reasignación de ejemplo**, hecha por el administrador inicial: «Diana reportó falla mecánica; Carlos cubre su R2». Le agrega la `R2` a Carlos, así que su programación queda `[R1, R2]`.

Con solo arrancar, el monitoreo ya muestra 2 buses, y conductores y rutas aparecen «en recorrido».

> **Importante: la semilla usa «hoy».** La programación y los recorridos se siembran para la fecha del arranque. Si vuelve a arrancar otro día, la programación sembrada queda en el pasado: los recorridos siguen activos, pero ya no hay programación de hoy. Para la demo, lo más simple es borrar el volumen y volver a arrancar:
>
> ```bash
> docker compose down -v
> ./gradlew bootRun --args='--spring.profiles.active=demo'
> ```
>
> También puede rehacer la programación de hoy con `PUT /api/v1/conductores/{id}/programacion`. Los ids de las tablas solo coinciden con los de esta sección sobre una base recién creada.

Cada grupo (conductores, vehículos, rutas, programación, recorridos, reasignación) se siembra solo si su tabla está vacía. La programación, los recorridos y la reasignación se crean con los mismos servicios que usa la API, así que cumplen las mismas reglas.

Las coordenadas de las rutas son **aproximadas**. Para reemplazarlas, siga las instrucciones de `demo/RutasDemo.kt`: se dibujan en geojson.io, o se copian del `FakeData.kt` de la app del pasajero cuando exista.

### URLs

| Qué | URL |
| --- | --- |
| API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

**Probar desde Swagger:** ejecute `POST /api/v1/auth/login` con las credenciales de abajo. El navegador guarda la cookie `JSESSIONID`, y con eso los demás endpoints ya funcionan desde la misma pestaña.

## Guion de demo por Swagger

Requiere el perfil `demo` sobre una base recién creada (`docker compose down -v` antes de arrancar). Abra `http://localhost:8080/swagger-ui.html` y siga los pasos en orden.

1. **Iniciar sesión.** En `POST /api/v1/auth/login` envíe:
   ```json
   { "correo": "admin@urbanmovil.co", "contrasena": "Admin12345" }
   ```
   Responde 200 con el administrador, y desde ahí la cookie queda en el navegador.
2. **Ver conductores.** `GET /api/v1/conductores` muestra en cada conductor su `rutasHoy`, y en Andrés y Laura un `enRecorrido` con datos.
3. **Ver el monitoreo.** `GET /api/v1/monitoreo/buses` devuelve 2 buses con `simulado: true`. Ejecútelo otra vez a los pocos segundos: la `posicion` avanzó sobre el recorrido principal.
4. **Reasignar.** A Laura le falla la R2 y le pasan la R3. En `POST /api/v1/conductores/2/reasignaciones` envíe:
   ```json
   { "motivo": "Laura reportó un problema en la R2; toma la R3", "rutasAsignar": [3], "rutasQuitar": [2] }
   ```
   Responde 201. Como Laura está recorriendo la R2, esa ruta queda en `RETIRO_PENDIENTE` (D-11), y la R3 se agrega al final de su programación.
5. **Finalizar el recorrido.** `POST /api/v1/demo/recorridos/2/finalizar` termina el recorrido de Laura. Después:
   - `GET /api/v1/conductores/2/programacion` ya no tiene la R2 y solo queda la R3.
   - El monitoreo muestra 1 bus (CA-31.4).
6. **Iniciar un recorrido.** En `POST /api/v1/demo/recorridos` envíe:
   ```json
   { "conductorId": 2, "rutaId": 3, "vehiculoId": 2 }
   ```
   Laura sale en la R3 con el `UM-118`, y el monitoreo vuelve a mostrar 2 buses.
7. **Probar una regla.** Repita el paso 6 con `"vehiculoId": 5`, que es el `UM-410` inactivo. Responde 409: «El vehículo está inactivo y no puede usarse en recorridos» (RN-25).
8. **Ver el historial.** `GET /api/v1/reasignaciones` lista las reasignaciones, de la más reciente a la más antigua, con su motivo, fecha y hora, administrador y rutas asignadas y quitadas.
9. **(Opcional) Reglas con recorrido activo.** Intente `DELETE /api/v1/conductores/1`, `DELETE /api/v1/rutas/1` o `PATCH /api/v1/vehiculos/1/estado` con `{ "activo": false, "motivo": "x" }`. Las tres responden 409 porque Andrés sigue en recorrido.

## Credenciales por defecto

| Usuario | Correo | Contraseña |
| --- | --- | --- |
| Administrador inicial | `admin@urbanmovil.co` | `Admin12345` |
| Conductores demo | (ver Swagger: `GET /conductores`) | `Conductor123` |

El administrador inicial se crea al arrancar solo si la tabla `administrador` está vacía (D-04). No hay endpoint para registrar administradores. Los conductores **no** pueden iniciar sesión en el panel web. Puede cambiar estos valores con variables de entorno **antes del primer arranque**:

| Variable | Por defecto |
| --- | --- |
| `ADMIN_NOMBRE` | `Administrador` |
| `ADMIN_CORREO` | `admin@urbanmovil.co` |
| `ADMIN_CONTRASENA` | `Admin12345` |
| `CORS_ORIGENES` | `http://localhost:5173` (varios orígenes separados por coma) |

La velocidad del bus simulado se configura con `urbanmovil.simulacion.velocidad-kmh` (por defecto `18`).

## Pruebas

Con Docker encendido:

```bash
./gradlew test
```

- **Integración** (`@SpringBootTest` + MockMvc + PostgreSQL real con Testcontainers):
  - Como mínimo una prueba por criterio de aceptación de HU-01 a HU-09, HU-25 y HU-28 a HU-31.
  - Pruebas de punta a punta de las reglas que dependen de recorridos activos (D-05, D-08, D-11 y RN-26), con recorridos creados por los endpoints de demo.
- **Unitarias** de `SimuladorPosicion`, sin Spring.
- **Hora fija:** la fija un `Clock` de prueba (`RelojPrueba`), que pone «hoy» en el 24 de noviembre de 2026 a las 10:00 en Bogotá. Las pruebas pueden adelantarlo.
- **Contextos:** las pruebas corren con `urbanmovil.demo.habilitado=true`. `DemoDeshabilitadoTest` usa un segundo contexto con la demo apagada, para verificar los 404, y por eso levanta un segundo contenedor.

## Endpoints

| Método y ruta | Descripción |
| --- | --- |
| `POST /api/v1/auth/login` | Inicia sesión (público) |
| `POST /api/v1/auth/logout` | Cierra la sesión (204) |
| `GET /api/v1/auth/me` | Administrador de la sesión actual |
| `GET /api/v1/conductores?q=&disponible=` | Lista conductores, con `enRecorrido` y `rutasHoy` |
| `GET /api/v1/conductores/{id}` | Detalle de un conductor |
| `POST /api/v1/conductores` | Registra un conductor (201) |
| `PUT /api/v1/conductores/{id}` | Modifica los datos (no cambia la disponibilidad) |
| `DELETE /api/v1/conductores/{id}` | Elimina un conductor (204) |
| `PATCH /api/v1/conductores/{id}/disponibilidad` | Lo marca disponible o no disponible, con motivo |
| `GET /api/v1/conductores/{id}/programacion?fecha=` | Programación del conductor (por defecto hoy) |
| `PUT /api/v1/conductores/{id}/programacion?fecha=` | Reemplaza la programación: `{ "rutas": [3, 1, 2] }` |
| `GET /api/v1/programaciones?fecha=` | Resumen del día de todos los conductores |
| `POST /api/v1/conductores/{id}/reasignaciones` | Reasigna rutas de hoy por una eventualidad (201) |
| `GET /api/v1/reasignaciones?conductorId=&fecha=` | Historial de reasignaciones, de la más reciente a la más antigua |
| `GET /api/v1/vehiculos?q=&activo=` | Lista vehículos |
| `GET /api/v1/vehiculos/{id}` | Detalle de un vehículo |
| `POST /api/v1/vehiculos` | Registra un vehículo (201) |
| `PUT /api/v1/vehiculos/{id}` | Modifica la placa y el número interno |
| `DELETE /api/v1/vehiculos/{id}` | Elimina un vehículo (204) |
| `PATCH /api/v1/vehiculos/{id}/estado` | Lo desactiva (con motivo) o lo reactiva |
| `GET /api/v1/rutas?q=` | Lista resumida de rutas, con `recorridosActivos` y `conductoresHoy` |
| `GET /api/v1/rutas/{id}` | Detalle: recorrido principal, `inicio`, `final` y alternos |
| `POST /api/v1/rutas` | Registra una ruta con su recorrido y sus alternos (201) |
| `PUT /api/v1/rutas/{id}` | Reemplaza la ruta completa, incluida la lista de alternos |
| `DELETE /api/v1/rutas/{id}` | Elimina una ruta (204) |
| `GET /api/v1/public/rutas` | **Público, sin sesión.** Rutas para la app del pasajero, ordenadas por código |
| `GET /api/v1/monitoreo/buses` | Buses en recorrido con posición **simulada** |
| `GET /api/v1/demo/recorridos?estado=` | **Solo perfil demo.** Lista los recorridos |
| `POST /api/v1/demo/recorridos` | **Solo perfil demo.** Inicia un recorrido: `{ conductorId, rutaId, vehiculoId }` (201) |
| `POST /api/v1/demo/recorridos/{id}/finalizar` | **Solo perfil demo.** Finaliza un recorrido |

### Rutas (Fase 2)

Un trazado es un arreglo de puntos en el **sentido de circulación**: el primero es el inicio y el último el final.

```json
{
  "codigo": "R1",
  "nombre": "Centro – Campanario",
  "color": "#0057B8",
  "recorridoPrincipal": [ { "lat": 2.4419, "lng": -76.6066 }, { "lat": 2.4462, "lng": -76.6031 } ],
  "alternos": [ { "nombre": "Desvío por Cra 9", "puntos": [ { "lat": 2.4419, "lng": -76.6066 }, { "lat": 2.4462, "lng": -76.6031 } ] } ]
}
```

- El `codigo` se guarda en mayúsculas. El código y el nombre son únicos, sin distinguir mayúsculas.
- El `color` es opcional y va en formato `#RRGGBB`. Si no llega, se asigna uno de una paleta fija de 6 colores, en orden de creación.
- El recorrido principal necesita 2 puntos o más, igual que cada alterno. El límite es de 2.000 puntos por trazado y 10 alternos por ruta.
- En el `PUT`, un alterno con `id` se actualiza, uno sin `id` se crea y uno que no viene se elimina. Si algo es inválido, no cambia nada.
- No se puede eliminar una ruta con recorridos activos. Al eliminarla se borran sus alternos, sus filas de programación y reasignación y sus recorridos finalizados (D-17).
- `GET /api/v1/public/rutas` lee siempre la base de datos, así que refleja los cambios al instante. Es la forma de verificar en este MVP que «los pasajeros ven la ruta» (V-3).

### Programación del día (HU-09)

- La programación se guarda **por fecha**: por defecto es la de hoy y se permiten días futuros (V-5). Una fecha pasada responde 409.
- `PUT` recibe la lista **completa y ordenada** de ids de ruta y la reemplaza:
  - Las rutas que salen de la lista se borran.
  - Las que siguen conservan su `retiro_pendiente`.
  - No se puede retirar la ruta que el conductor está recorriendo (409).
  - Una ruta repetida responde 400.
- El `estado` de cada ruta programada puede ser `PENDIENTE`, `EN_RECORRIDO` o `RETIRO_PENDIENTE`.
- No se exige que el conductor esté disponible para programarlo (HU-09 no lo pide).

### Reasignaciones (HU-25)

- Aplican sobre la programación de **hoy**. Si el conductor no tiene programación de hoy, se crea.
- Las rutas de `rutasAsignar` se agregan al final, en el orden recibido.
- Las rutas de `rutasQuitar` salen de la programación. Si el conductor está recorriendo una de ellas, esa queda en `RETIRO_PENDIENTE` y sale al finalizar el recorrido (D-11).
- Se guarda quién la hizo (el administrador de la sesión), cuándo, el motivo y qué rutas se asignaron y quitaron. Todo pasa en una sola transacción, y no toca la programación de los demás conductores.

### Recorridos de demostración (D-16)

La app del conductor todavía no existe, así que los recorridos se inician y finalizan desde Swagger, **solo con el perfil `demo`**. Para iniciar un recorrido:

- el conductor debe estar disponible;
- la ruta debe estar en su programación de hoy, sin retiro pendiente;
- el vehículo debe estar activo;
- ni el conductor ni el vehículo pueden estar en otro recorrido activo.

### Monitoreo simulado (HU-31)

No hay GPS real: la respuesta siempre trae `simulado: true`. La posición se calcula **solo con el recorrido principal**, nunca con los alternos (RN-03), y depende de la hora:

1. Se calcula la longitud del recorrido principal sumando las distancias haversine entre puntos consecutivos.
2. Se calcula cuánto dura una vuelta: longitud ÷ velocidad (18 km/h por defecto).
3. Con el tiempo transcurrido desde el inicio del recorrido, se obtiene en qué fracción de la vuelta va el bus.
4. Se interpola linealmente el punto que está en esa fracción de la longitud.

Por eso, todos los que consultan a la misma hora ven la misma posición. El cálculo está en `monitoreo/SimuladorPosicion.kt`, una clase pura con pruebas unitarias.

### Formato de errores

Todos los errores tienen el mismo formato:

```json
{ "codigo": "DATO_REPETIDO", "mensaje": "Ya existe un conductor con este correo", "campos": [ { "campo": "correo", "mensaje": "Ya existe un conductor con este correo" } ] }
```

Los códigos posibles son `VALIDACION` (400), `NO_AUTENTICADO` (401), `CREDENCIALES_INVALIDAS` (401), `NO_ENCONTRADO` (404), `DATO_REPETIDO` (409) y `REGLA_NEGOCIO` (409).

## Decisiones provisionales

Están pendientes de confirmación del equipo o del cliente. Cambiarlas no exige rediseñar nada.

| Decisión | Qué se implementó |
| --- | --- |
| **D-08** | No se puede marcar como no disponible a un conductor que tiene un recorrido activo. |
| **D-11** | Una ruta que se quita mientras el conductor la recorre queda en `RETIRO_PENDIENTE` y sale de la programación al finalizar ese recorrido. |
| **D-14** | Un vehículo solo tiene placa y número interno. |
| **D-15** | El motivo de desactivación de un vehículo es texto libre de máximo 300 caracteres. |
| **D-17** | Al eliminar definitivamente un conductor, un vehículo o una ruta sin recorrido activo, la base de datos borra en cascada su historial: programación, reasignaciones y recorridos finalizados. |
| **V-5** | La programación se guarda por fecha: por defecto es la de hoy y se permiten días futuros. |
| **V-6** | Si el motivo de no disponibilidad es `OTRO`, el detalle es obligatorio. |
| D-07 | El mínimo de 8 caracteres de la contraseña es provisional. |

## Simplificaciones del MVP

Son decisiones conscientes para un MVP académico. Hay que revisarlas antes de un despliegue real.

- **Sesión de servidor, sin JWT.** El login crea una sesión HTTP con la cookie `JSESSIONID` (`HttpOnly`, `SameSite=Lax`, vence a las 8 h). La sesión vive en memoria: si se reinicia la app, hay que volver a iniciar sesión, y no sirve para varias instancias.
- **CSRF deshabilitado en `/api/**`.** Lo mitigan la cookie `SameSite=Lax`, que el navegador no envía en `POST`/`PUT`/`PATCH`/`DELETE` iniciados desde otro sitio, y el CORS restringido a `urbanmovil.cors.origenes` con credenciales. En producción conviene activar CSRF con token, o al menos `SameSite=Strict`.
- **Contraseñas** con BCrypt.
- **Monitoreo simulado:** los recorridos los crean los endpoints de demostración (D-16) y la posición se calcula según la hora. No hay GPS.
- **Listas sin paginación**, porque el volumen del MVP es pequeño.
- Si se desactiva un vehículo que ya estaba inactivo, se actualiza el motivo y se conserva la fecha original de inactivación.

## Estructura

```
src/main/kotlin/co/urbanmovil/api
├─ config/        Seguridad, CORS, OpenAPI, Clock, Jackson (recorte de espacios), propiedades
├─ common/        Formato de errores, excepciones, manejador global, utilidades de texto
├─ auth/          Login, logout, sesión actual y administrador inicial
├─ administrador/ Entidad y repositorio
├─ conductor/     HU-06, HU-07, HU-08
├─ vehiculo/      HU-28, HU-29, HU-30
├─ ruta/          HU-02, HU-03, HU-04, HU-05 y endpoint público
├─ programacion/  HU-09: programación del día
├─ reasignacion/  HU-25: reasignaciones con historial
├─ recorrido/     Entidad y consultas de «recorrido activo»
├─ monitoreo/     HU-31: SimuladorPosicion y endpoint de buses
└─ demo/          Datos semilla y recorridos de demostración (D-16)
```

«Hoy» y «ahora» se calculan siempre con un bean `Clock` en la zona `America/Bogota`, y las fechas se devuelven en ISO-8601 con zona `-05:00`.
