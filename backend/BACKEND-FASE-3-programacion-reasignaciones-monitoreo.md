# UrbanMovil · Backend administrativo · Fase 3 de 3 — Programación, reasignaciones, recorridos de demostración y monitoreo

> Documento de implementación para el agente de desarrollo. **Requiere las fases 1 y 2 terminadas**. Implementar solo esta fase. Si algo contradice las HU de `Urban_Mobility_HU_v1.2.0.xlsx`, detente y pregunta.

## 1. Contexto breve

Backend Spring Boot 4 + Kotlin + PostgreSQL del panel administrativo de UrbanMovil (MVP académico, una sola empresa de buses de Popayán). Siguen vigentes las convenciones de la Fase 1 (capas, DTOs, `/api/v1`, formato de errores, español, `Clock` inyectable, zona `America/Bogota`, Testcontainers). Las tablas `programacion`, `programacion_ruta`, `reasignacion`, `reasignacion_ruta` y `recorrido` ya existen desde `V1`.

La app del conductor (EP-02) **no existe**. Por eso los recorridos activos se crean con endpoints de demostración sin pantalla (decisión **D-16**, aprobada por el cliente), y el monitoreo muestra posiciones **simuladas**.

«Hoy» = fecha actual en `America/Bogota` según el `Clock`.

## 2. Objetivo de esta fase

1. Programación del día por conductor (HU-09).
2. Reasignaciones por eventualidad con historial (HU-25).
3. Endpoints de demostración para iniciar y finalizar recorridos (D-16).
4. Monitoreo de buses con posición simulada (HU-31).
5. Completar campos que las fases anteriores dejaron vacíos: `rutasHoy` en conductores y `conductoresHoy` en rutas.

## 3. Programación del día (HU-09)

**HU-09** — Como administrador de la empresa, quiero asignar a un conductor, antes de su primer recorrido, las rutas que hará en el día y el orden en que las hará, para que el conductor sepa qué rutas tiene y el sistema pueda pasarlo de una a la siguiente.

### 3.1 Endpoints

| Método y ruta | Descripción |
| --- | --- |
| `GET /api/v1/conductores/{id}/programacion?fecha=YYYY-MM-DD` | `fecha` opcional (por defecto hoy). Respuesta: `{ conductorId, fecha, rutas: [{ rutaId, codigo, nombre, color, orden, estado }] }` ordenadas por `orden`. `estado`: `EN_RECORRIDO` (hay recorrido `ACTIVO` del conductor en esa ruta), `RETIRO_PENDIENTE` (`retiro_pendiente = true`) o `PENDIENTE`. Si no hay programación, `rutas` vacía |
| `PUT /api/v1/conductores/{id}/programacion?fecha=YYYY-MM-DD` | Cuerpo `{ "rutas": [3, 1, 2] }` = lista **completa y ordenada** de ids de ruta. Crea la programación si no existe y la reemplaza respetando las reglas de §3.2. Responde como el `GET` |
| `GET /api/v1/programaciones?fecha=` | Resumen del día para la pantalla: `[{ conductorId, nombre, disponible, enRecorrido, totalRutas }]` de todos los conductores |

### 3.2 Reglas y criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-09.1 | Conductor sin recorridos en el día y 2 rutas registradas | `PUT` con `[R2, R1]` | 200; el `GET` las devuelve en ese orden (`orden` 1 y 2) |
| CA-09.2 | Conductor con una ruta asignada sin recorrido activo en ella | `PUT` sin esa ruta | La ruta deja de estar en la programación |
| CA-09.3 | Conductor con recorrido `ACTIVO` en la ruta X | `PUT` sin la ruta X | 409 `REGLA_NEGOCIO` «No se puede retirar la ruta X: el conductor la está recorriendo. Se podrá retirar cuando termine.» No cambia nada |
| CA-09.4 | Lista con una ruta repetida | `PUT` | 400 `VALIDACION` «La ruta X está repetida en la programación»; nunca se crean asignaciones duplicadas |
| CA-09.5 | Conductor con programación y sin recorrido activo | `PUT` con otro orden | 200; queda el nuevo orden |
| — | Una ruta que no existe en la lista | `PUT` | 404 `NO_ENCONTRADO` |
| — | Fecha anterior a hoy | `PUT` | 409 `REGLA_NEGOCIO` «No se puede modificar la programación de un día pasado» (propuesta de la IA) |

Notas:

- La programación se guarda **por fecha** (vacío V-5, propuesta: hoy por defecto, se permiten días futuros).
- No se exige que el conductor esté disponible para programarlo; HU-09 no lo dice. Si el equipo lo pide, se agrega después.
- El `PUT` conserva `retiro_pendiente` de las rutas que siguen en la lista.

## 4. Reasignación por eventualidad (HU-25)

**HU-25** — Como administrador de la empresa, quiero reasignar las rutas de un conductor durante el día, escribiendo qué pasó e indicando qué rutas se le asignan y cuáles se le quitan, para cubrir una ruta cuando ocurre una eventualidad y dejar constancia del motivo.

### 4.1 Endpoints

| Método y ruta | Descripción |
| --- | --- |
| `POST /api/v1/conductores/{id}/reasignaciones` | `{ "motivo": "Diana reportó falla mecánica", "rutasAsignar": [2], "rutasQuitar": [1] }`. Aplica sobre la programación de **hoy**. 201 `{ id, fechaHora, administrador: { id, nombre }, motivo, rutasAsignadas: [codigo], rutasQuitadas: [codigo], programacion }` |
| `GET /api/v1/reasignaciones?conductorId=&fecha=` | Historial, más reciente primero. Ambos filtros opcionales; `fecha` filtra por día de `fecha_hora` en Bogotá |

### 4.2 Efecto sobre la programación de hoy

1. Si el conductor no tiene programación hoy, se crea.
2. Cada ruta de `rutasAsignar` se agrega **al final**, en el orden recibido.
3. Cada ruta de `rutasQuitar`:
   - sin recorrido `ACTIVO` del conductor en ella → se elimina de la programación;
   - con recorrido `ACTIVO` → se marca `retiro_pendiente = true` y se elimina al finalizar ese recorrido (CA-25.3, D-11 provisional).
4. Se guardan `reasignacion` (con el administrador de la sesión y `fecha_hora = ahora`) y una fila de `reasignacion_ruta` por ruta (`ASIGNADA` o `QUITADA`).
5. Todo en una sola transacción.
6. **No** se modifica nada de otros conductores ni se recalcula nada (CA-25.4, RN-24).

### 4.3 Reglas y criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-25.1 | Conductor con programación del día | Reasignación válida | 201; programación actualizada y reasignación registrada con motivo, fecha, hora y administrador |
| CA-25.2 | `motivo` vacío o solo espacios | `POST` | 400 `VALIDACION` «Escriba qué pasó» |
| CA-25.3 | Recorrido `ACTIVO` en una ruta de `rutasQuitar` | `POST` | 201; esa ruta queda con `estado = RETIRO_PENDIENTE` y sale de la programación al finalizar el recorrido |
| CA-25.4 | Reasignación guardada | Se consultan las programaciones de los demás conductores | No cambiaron |
| CA-25.5 | `rutasAsignar` y `rutasQuitar` vacías | `POST` | 400 `VALIDACION` «Indique al menos una ruta para asignar o quitar» |
| — | Ruta en `rutasAsignar` que ya está en la programación | `POST` | 409 `REGLA_NEGOCIO` «La ruta X ya está en la programación» |
| — | Ruta en `rutasQuitar` que no está en la programación | `POST` | 409 `REGLA_NEGOCIO` «La ruta X no está en la programación» |
| — | Misma ruta en ambas listas | `POST` | 400 `VALIDACION` |

## 5. Recorridos de demostración (D-16)

Solo existen cuando `urbanmovil.demo.habilitado = true` (perfil `demo`); si no, los endpoints responden 404. Requieren sesión de administrador. No tienen pantalla: se usan desde Swagger.

| Método y ruta | Descripción |
| --- | --- |
| `GET /api/v1/demo/recorridos?estado=ACTIVO` | Lista `{ id, conductor, ruta, vehiculo, estado, inicio, fin }` |
| `POST /api/v1/demo/recorridos` | `{ conductorId, rutaId, vehiculoId }` → 201. `estado = ACTIVO`, `inicio = ahora` |
| `POST /api/v1/demo/recorridos/{id}/finalizar` | 200. `estado = FINALIZADO`, `fin = ahora`. Si la ruta de ese recorrido tenía `retiro_pendiente = true` en la programación de hoy del conductor, se elimina de la programación (D-11) |

Validaciones al iniciar (todas 409 `REGLA_NEGOCIO` salvo donde se indique):

| Regla | Mensaje |
| --- | --- |
| Conductor no disponible (CA-08.1) | «El conductor no está disponible» |
| La ruta no está en la programación de hoy del conductor, o tiene `retiro_pendiente` | «La ruta no está en la programación de hoy del conductor» |
| Vehículo inactivo (RN-25) | «El vehículo está inactivo y no puede usarse en recorridos» |
| El conductor ya tiene un recorrido activo | «El conductor ya tiene un recorrido activo» |
| El vehículo ya está en otro recorrido activo | «El vehículo ya está en otro recorrido activo» |
| Finalizar un recorrido ya finalizado | «El recorrido ya está finalizado» |

## 6. Monitoreo simulado (HU-31)

**HU-31** — Como administrador de la empresa, quiero ver en un mapa los buses que tienen un recorrido activo, con su vehículo, ruta, conductor y hora de la última posición, para tener visibilidad de la operación de la empresa durante el piloto.

### 6.1 Endpoint

`GET /api/v1/monitoreo/buses` →

```json
{
  "simulado": true,
  "generadoEn": "2026-11-24T19:42:18-05:00",
  "buses": [
    {
      "recorridoId": 12,
      "vehiculo": { "id": 1, "numeroInterno": "UM-104", "placa": "ABC123" },
      "ruta": { "id": 1, "codigo": "R1", "nombre": "Centro – Campanario", "color": "#1F4FD8" },
      "conductor": { "id": 3, "nombre": "Carlos Muñoz" },
      "posicion": { "lat": 2.4431, "lng": -76.6052 },
      "horaPosicion": "2026-11-24T19:42:18-05:00"
    }
  ]
}
```

El front lo consultará cada 3–5 segundos (CA-31.2). Las geometrías de las rutas las toma de `GET /api/v1/rutas/{id}`.

### 6.2 Cálculo de la posición simulada

Solo con el **recorrido principal** (RN-03), nunca con los alternos:

1. `longitud` = suma de distancias haversine entre puntos consecutivos del recorrido principal (metros).
2. `velocidad` = `urbanmovil.simulacion.velocidad-kmh` (nueva propiedad, por defecto `18`).
3. `duracionVuelta` = `longitud / velocidad`.
4. `transcurrido` = `ahora − recorrido.inicio`.
5. `fraccion` = `(transcurrido mod duracionVuelta) / duracionVuelta`.
6. `posicion` = punto a esa fracción de la longitud, interpolando linealmente entre los dos puntos del tramo.
7. `horaPosicion` = `ahora`.

Pon este cálculo en una clase pura (`SimuladorPosicion`) con pruebas unitarias. Es la misma idea que la app del pasajero (posición según la hora), así que todos los que miren el monitoreo ven lo mismo.

### 6.3 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-31.1 | Uno o más recorridos `ACTIVO` | `GET /monitoreo/buses` | Un elemento por recorrido con vehículo, ruta, conductor, posición y hora |
| CA-31.2 | Mismo recorrido | Dos llamadas separadas unos segundos | La posición avanzó sobre el recorrido principal |
| CA-31.3 | Ningún recorrido activo | `GET` | `buses: []` (el front muestra «No hay buses en recorrido») |
| CA-31.4 | Un recorrido que se finaliza | `GET` | Ya no aparece |
| CA-31.5 | — | `GET` | `simulado: true` (el front muestra el aviso) |

## 7. Completar campos de fases anteriores

- `GET /api/v1/conductores`: `rutasHoy` = códigos de la programación de hoy en orden; `enRecorrido` con datos reales.
- `GET /api/v1/rutas`: `conductoresHoy` = nombres de los conductores que la tienen en su programación de hoy.

## 8. Datos semilla (perfil `demo`)

Añade al sembrador, si las tablas están vacías:

- Programación de hoy: conductor 1 → `[R1, R2]`; conductor 2 → `[R2]`; conductor 3 → `[R1]`.
- 2 recorridos `ACTIVO` iniciados hace unos minutos: conductor 1 en R1 con `UM-104`; conductor 2 en R2 con `UM-305`.
- 1 reasignación de ejemplo hecha por el administrador inicial (motivo: «Diana reportó falla mecánica; Carlos cubre su R2»).

Como la semilla usa «hoy», al arrancar otro día la programación sembrada queda en el pasado: documenta en el README que para la demo basta con borrar el volumen de Docker y volver a arrancar, o usar los endpoints.

## 9. Pruebas

- Una prueba de integración por cada CA de §3.2, §4.3, §5 y §6.3 con resultado verificable en el backend.
- Pruebas de punta a punta de las reglas que dependen de recorridos activos, ahora creados con los endpoints de demo:
  - D-05: no eliminar conductor en recorrido (CA-07.3) y no eliminar ruta con recorrido (CA-05.2).
  - D-08: no marcar no disponible en recorrido (CA-08.4).
  - RN-26: no eliminar ni desactivar vehículo en recorrido (CA-29.3, CA-30.3).
  - D-11: quitar una ruta en curso → `RETIRO_PENDIENTE` → finalizar → la ruta sale de la programación.
- Pruebas unitarias de `SimuladorPosicion`: fracción 0 → primer punto; fracción 0.5 → punto medio por distancia; vuelta completa → vuelve al inicio.
- Fija la hora con un `Clock` de prueba.

## 10. Definición de terminado (Fase 3 y backend)

- [ ] Programación del día según §3.
- [ ] Reasignaciones con historial según §4.
- [ ] Recorridos de demostración solo con el perfil `demo`, con todas las validaciones de §5.
- [ ] Monitoreo simulado según §6.
- [ ] Campos `rutasHoy` y `conductoresHoy` completos.
- [ ] Semilla `demo` completa: con solo arrancar se ven buses en el monitoreo y estados «en recorrido» en conductores y rutas.
- [ ] `./gradlew test` en verde (fases 1, 2 y 3).
- [ ] README con un **guion de demo por Swagger** (login → ver conductores → reasignar → iniciar y finalizar un recorrido → ver monitoreo) y la lista de decisiones provisionales (D-08, D-11, D-14, D-15, D-17, V-5, V-6).

## 11. Fuera del alcance

App del conductor (EP-02), GPS real, notificaciones, reportes, auditoría más allá de la tabla `reasignacion`, despliegue en la nube.

## 12. Trazabilidad

| Elemento | HU | Reglas / decisiones |
| --- | --- | --- |
| Programación del día | HU-09 | RN-06, RN-20, D-05, RF-25, RF-29, V-5 |
| Reasignaciones | HU-25 | RN-23, RN-24, D-05, D-11, RF-33 |
| Recorridos de demostración | — (soporte) | D-16, RN-25, D-05, D-08, D-11 |
| Monitoreo simulado | HU-31 | RN-03, D-16, RF-35 |
