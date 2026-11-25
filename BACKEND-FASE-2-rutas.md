# UrbanMovil · Backend administrativo · Fase 2 de 3 — Rutas y caminos alternos

> Documento de implementación para el agente de desarrollo. **Requiere la Fase 1 terminada** (proyecto, esquema completo en `V1`, seguridad, formato de errores, perfil `demo`). Implementar solo esta fase. Si algo contradice las HU de `Urban_Mobility_HU_v1.2.0.xlsx`, detente y pregunta.

## 1. Contexto breve

Backend Spring Boot 4 + Kotlin + PostgreSQL del panel administrativo de UrbanMovil (MVP académico, una sola empresa de buses de Popayán). Siguen vigentes todas las convenciones de la Fase 1: paquetes por funcionalidad, `Controller → Service → Repository`, DTOs, prefijo `/api/v1`, errores en el formato de §3.4 de la Fase 1, mensajes en español, zona `America/Bogota` y pruebas con Testcontainers.

La tabla `ruta` y la tabla `camino_alterno` **ya existen** desde `V1`. No crees migraciones de estructura nuevas salvo que encuentres un error en `V1`; en ese caso usa `V2__...sql` y explícalo en el README.

## 2. Objetivo de esta fase

1. CRUD de rutas con recorrido principal y caminos alternos (HU-02, HU-03, HU-04, HU-05).
2. Endpoint público de rutas para la futura app del pasajero.
3. Rutas en los datos semilla del perfil `demo`.

## 3. Modelo

```kotlin
data class Punto(val lat: Double, val lng: Double)
```

- `ruta.recorrido_principal`: lista ordenada de `Punto` en el **sentido de circulación**. El primer punto es el **inicio** y el último el **final** (CA-02.5). No hay columnas aparte para inicio y final.
- `camino_alterno.puntos`: lista ordenada de `Punto`.
- `ruta.codigo`: código corto visible (p. ej. `R1`), propuesta de la IA para mostrarlo como en la app del pasajero.
- `ruta.color`: color hex `#RRGGBB`; si no llega, asigna uno de una paleta fija de 6 colores en orden de creación.

## 4. Validaciones

| Campo | Regla | Mensaje |
| --- | --- | --- |
| `codigo` | Obligatorio, máx. 10, único (sin distinguir mayúsculas), se guarda en mayúsculas | «Ya existe una ruta con este código» |
| `nombre` | Obligatorio, máx. 120, único (sin distinguir mayúsculas) | «Ya existe una ruta con este nombre» (CA-02.3) |
| `color` | Opcional, formato `#RRGGBB` | «Color inválido» |
| `recorridoPrincipal` | Obligatorio y con **2 puntos o más** | Si falta o está vacío: «Falta trazar el recorrido principal» (CA-02.2, CA-04.2). Si tiene 1 punto: «La ruta debe tener punto de inicio y punto final» (CA-02.4) |
| Cada `Punto` | `lat` entre -90 y 90; `lng` entre -180 y 180 | «Coordenadas inválidas» |
| `alternos[]` | Opcional; cada alterno con `nombre` (obligatorio, máx. 120) y 2 puntos o más | «Cada camino alterno debe tener al menos dos puntos» |
| Tamaño | Máximo 2.000 puntos por trazado y 10 alternos por ruta (límite técnico del MVP) | «El trazado supera el máximo de puntos permitido» |

Todas las validaciones de trazados responden 400 `VALIDACION` con el campo en `campos` (p. ej. `recorridoPrincipal`, `alternos[1].puntos`).

## 5. Endpoints

| Método y ruta | Entrada / salida | HU |
| --- | --- | --- |
| `GET /api/v1/rutas?q=` | Lista resumida: `id, codigo, nombre, color, puntosRecorrido` (cantidad), `numAlternos, recorridosActivos` (cantidad de `recorrido` en `ACTIVO`), `conductoresHoy` (nombres; lista vacía hasta la Fase 3), `actualizadoEn` | HU-04, HU-05 |
| `GET /api/v1/rutas/{id}` | Detalle completo: lo anterior + `recorridoPrincipal`, `inicio`, `final` (primer y último punto), `alternos: [{ id, nombre, puntos }]` | HU-02, HU-03 |
| `POST /api/v1/rutas` | `{ codigo, nombre, color?, recorridoPrincipal: [Punto], alternos?: [{ nombre, puntos }] }` → 201 detalle | HU-02, HU-03 |
| `PUT /api/v1/rutas/{id}` | Mismo cuerpo que `POST`. **Reemplaza** nombre, código, color, recorrido y la lista completa de alternos: un alterno que no viene se elimina (CA-04.3); uno con `id` se actualiza; uno sin `id` se crea | HU-03, HU-04 |
| `DELETE /api/v1/rutas/{id}` | 204 | HU-05 |
| `GET /api/v1/public/rutas` | **Sin autenticación.** `[{ codigo, nombre, color, recorridoPrincipal, alternos: [{ nombre, puntos }] }]` ordenado por `codigo` | CA-02.1, CA-04.1, CA-05.1 |

El endpoint público es la forma de verificar en este MVP que «los pasajeros ven la ruta», porque la app del pasajero todavía usa datos quemados (vacío V-3). Debe reflejar los cambios inmediatamente.

## 6. Reglas y criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-02.1 | Nombre no repetido y recorrido trazado | `POST /rutas` | 201; la ruta aparece en `GET /public/rutas` |
| CA-02.2 | Sin recorrido principal | `POST /rutas` | 400 «Falta trazar el recorrido principal» |
| CA-02.3 | Ya existe una ruta con ese nombre | `POST /rutas` | 409 `DATO_REPETIDO` «Ya existe una ruta con este nombre» |
| CA-02.4 | Recorrido con un solo punto | `POST /rutas` | 400 «La ruta debe tener punto de inicio y punto final» |
| CA-02.5 | Recorrido con N puntos | Se guarda y se consulta | Los puntos vuelven en el mismo orden; `inicio` = primero, `final` = último |
| CA-03.1 | Una ruta registrada | Se guarda con un alterno | El alterno queda asociado y vuelve en el detalle y en `public` |
| CA-03.2 | Ruta con 2 o más alternos | Se consulta | Vuelven todos, separados del principal |
| CA-03.3 | — | — | Los alternos nunca se usan para cálculos; se respeta en la Fase 3 (monitoreo usa solo el principal) |
| CA-04.1 | Ruta registrada | `PUT` cambia nombre o trazado | 200; `GET /public/rutas` muestra la versión nueva |
| CA-04.2 | Ruta en edición | `PUT` sin recorrido principal | 400 «Falta trazar el recorrido principal»; no cambia nada |
| CA-04.3 | Ruta con 2 alternos | `PUT` envía solo uno | El otro se elimina; el enviado se conserva |
| CA-05.1 | Ruta sin recorridos `ACTIVO` | `DELETE` | 204; desaparece de `/rutas` y de `/public/rutas`; se borran sus alternos y sus filas de `programacion_ruta` (se retira de las asignaciones). Por D-17 (provisional) también se borran sus recorridos finalizados y sus filas de `reasignacion_ruta` |
| CA-05.2 | Ruta con al menos un recorrido `ACTIVO` | `DELETE` | 409 `REGLA_NEGOCIO` «No se puede eliminar: la ruta tiene recorridos activos.» |
| CA-05.3 | — | — | Cancelar la confirmación: responsabilidad del front |

## 7. Datos semilla (perfil `demo`)

Añade al sembrador de la Fase 1, si la tabla `ruta` está vacía, 3 rutas sobre calles reales de Popayán (coordenadas aproximadas, alrededor del Parque Caldas en `2.4419, -76.6066`), de 15 a 40 puntos cada una:

| Código | Nombre | Alternos |
| --- | --- | --- |
| R1 | Centro – Campanario | 1 («Desvío por Cra 9») |
| R2 | Bello Horizonte – Terminal | 0 |
| R3 | La Esmeralda – Pomona | 2 |

Si en el repositorio existe el `FakeData.kt` de la app del pasajero (proyecto Kotlin Multiplatform), **reutiliza sus mismas coordenadas** para que ambas apps muestren las mismas rutas. Si no, traza coordenadas razonables y deja un comentario indicando cómo reemplazarlas (p. ej. dibujando en geojson.io).

## 8. Pruebas

- Una prueba de integración por cada CA de §6 marcado con resultado verificable en el backend (todas salvo CA-03.3 y CA-05.3).
- Una prueba que confirme que `GET /api/v1/public/rutas` responde 200 sin sesión y que `GET /api/v1/rutas` responde 401 sin sesión.
- Una prueba de ida y vuelta del JSON del trazado (orden y precisión de las coordenadas se conservan).

## 9. Definición de terminado (Fase 2)

- [ ] CRUD de rutas con alternos según §5 y §6.
- [ ] `GET /public/rutas` público y actualizado al instante.
- [ ] El conteo `recorridosActivos` y la regla CA-05.2 consultan la tabla `recorrido`.
- [ ] Perfil `demo` siembra 3 rutas (y conserva lo de la Fase 1).
- [ ] Swagger documenta los endpoints nuevos con ejemplos de cuerpo.
- [ ] `./gradlew test` en verde (incluidas las pruebas de la Fase 1).
- [ ] README actualizado con los endpoints de rutas.

## 10. Trazabilidad

| Elemento | HU | Reglas / decisiones |
| --- | --- | --- |
| Crear ruta y recorrido principal | HU-02 | RN-01, RN-02, RN-03, RN-21, RF-18, RF-30 |
| Caminos alternos | HU-03 | RN-02, RN-03, RF-19 |
| Modificar ruta | HU-04 | RN-02, RF-20 |
| Eliminar ruta | HU-05 | RN-12, RF-21, D-17 |
| Endpoint público | CA-02.1, CA-04.1, CA-05.1 | V-3 |
