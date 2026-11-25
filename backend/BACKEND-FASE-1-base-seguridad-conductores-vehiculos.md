# UrbanMovil · Backend administrativo · Fase 1 de 3 — Base, seguridad, conductores y vehículos

> Documento de implementación para el agente de desarrollo. Implementar **solo esta fase**. Las fases 2 y 3 vienen en archivos aparte y se apoyan en lo que se construya aquí. Fuente de requisitos: `Urban_Mobility_HU_v1.2.0.xlsx` (épica EP-01). Ninguna HU está aprobada todavía; si algo de este documento contradice la HU, **detente y pregunta**, no decidas.

## 1. Contexto

UrbanMovil es una plataforma de transporte público para una sola empresa de buses de Popayán (Colombia). Este backend sirve a un **panel web de administración** (se construirá después, en React) y, más adelante, a las apps móviles. Es un **MVP académico**: prioriza que funcione, que las reglas de negocio se cumplan y que sea fácil de levantar, por encima de la escalabilidad.

Alcance del backend completo (3 fases):

| Fase | HU | Contenido |
| --- | --- | --- |
| **1 (este archivo)** | HU-01, HU-06, HU-07, HU-08, HU-28, HU-29, HU-30 | Proyecto base, esquema completo de BD, seguridad y login del administrador, conductores, disponibilidad, vehículos |
| 2 | HU-02, HU-03, HU-04, HU-05 | Rutas con recorrido principal y caminos alternos; endpoint público de rutas |
| 3 | HU-09, HU-25, HU-31 | Programación del día, reasignaciones, recorridos de demostración y monitoreo simulado |

Fuera del alcance: app del conductor (EP-02), app del pasajero (EP-03), GPS real, envío de correos.

## 2. Stack (ya inicializado con Spring Initializr)

El proyecto **ya existe**, creado con Spring Initializr. Trabaja sobre él; no lo regeneres.

| Elemento | Valor |
| --- | --- |
| Lenguaje | Kotlin |
| Build | Gradle (Kotlin DSL) |
| Framework | Spring Boot 4.x (la versión estable que generó Initializr) |
| JDK | 21 |
| Paquete base | `co.urbanmovil.admin` |
| Dependencias de Initializr | Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Spring Security, Validation, Docker Compose Support, Testcontainers, Spring Boot DevTools |
| Dependencia añadida a mano | `org.springdoc:springdoc-openapi-starter-webmvc-ui`, **línea 3.x** (la compatible con Spring Boot 4) |
| Base de datos | PostgreSQL en Docker (`compose.yaml` en la raíz, levantado automáticamente por Spring Boot al ejecutar `bootRun`) |
| Pruebas | JUnit 5 + Testcontainers (PostgreSQL) + `spring-security-test` |

Reglas:

- Usa versiones compatibles con las que ya trae el `build.gradle.kts`; no bajes versiones.
- **Prohibido**: Lombok, MapStruct, Redis, colas de mensajes, microservicios, JWT, OAuth, Keycloak, H2 para pruebas. Todo es un único servicio.
- Verifica que Initializr haya añadido los plugins `kotlin("plugin.spring")` y `kotlin("plugin.jpa")` (necesarios para entidades JPA en Kotlin). Si faltan, añádelos.

## 3. Arquitectura y convenciones

### 3.1 Estructura de paquetes (por funcionalidad)

```
co.urbanmovil.admin
├─ UrbanMovilAdminApplication.kt
├─ config/        // SecurityConfig, CorsConfig, OpenApiConfig, ClockConfig
├─ common/        // excepciones, manejador global de errores, utilidades de texto
├─ auth/          // login, logout, sesión actual, administrador inicial
├─ administrador/ // entidad y repositorio
├─ conductor/     // entidad, repositorio, servicio, controlador, DTOs
├─ vehiculo/
├─ ruta/          // Fase 2 (solo entidad y repositorio en Fase 1 si hacen falta)
├─ programacion/  // Fase 3
├─ reasignacion/  // Fase 3
├─ recorrido/     // entidad y repositorio desde Fase 1 (se consulta para reglas)
├─ monitoreo/     // Fase 3
└─ demo/          // datos semilla del perfil demo
```

Dentro de cada paquete: `Entidad.kt`, `EntidadRepository.kt`, `EntidadService.kt`, `EntidadController.kt`, `EntidadDtos.kt`.

### 3.2 Capas

- **Controller**: solo HTTP (rutas, códigos, `@Valid`). Sin reglas de negocio.
- **Service**: todas las reglas de negocio y transacciones (`@Transactional`). La API debe rechazar lo mismo aunque se llame sin pasar por la web.
- **Repository**: Spring Data JPA.
- **DTOs**: `data class` de entrada (`...Request`) y salida (`...Response`). **Nunca** se exponen entidades JPA en la API.

### 3.3 API

- Prefijo: `/api/v1`. JSON en `camelCase`. Textos y mensajes en **español**.
- Identificadores: `Long` autogenerados (`GENERATED ALWAYS AS IDENTITY`).
- Fechas y horas: se guardan como `timestamptz`; la API responde ISO-8601 con zona `-05:00`. «Hoy» siempre se calcula en zona **`America/Bogota`** usando un bean `Clock` inyectable (para poder fijar la hora en las pruebas).
- Listas sin paginación (el volumen del MVP es pequeño), con filtros opcionales por query string.
- Texto de entrada: se recortan espacios (`trim`) antes de validar. Las comparaciones de unicidad de texto no distinguen mayúsculas.

### 3.4 Formato de errores (para todos los endpoints)

Un `@RestControllerAdvice` convierte las excepciones a este cuerpo:

```json
{
  "codigo": "DATO_REPETIDO",
  "mensaje": "Ya existe un conductor con este número de identificación",
  "campos": [ { "campo": "identificacion", "mensaje": "Ya existe un conductor con este número de identificación" } ]
}
```

| Situación | HTTP | `codigo` |
| --- | --- | --- |
| Campo obligatorio vacío o formato inválido | 400 | `VALIDACION` (con `campos`) |
| Sin sesión | 401 | `NO_AUTENTICADO` |
| Credenciales incorrectas en login | 401 | `CREDENCIALES_INVALIDAS` |
| Recurso no existe | 404 | `NO_ENCONTRADO` |
| Valor único repetido | 409 | `DATO_REPETIDO` (con `campos`) |
| Regla de negocio bloquea la acción | 409 | `REGLA_NEGOCIO` |

La interfaz mostrará `mensaje` tal cual, así que debe ser una frase clara para el administrador.

## 4. Configuración

### 4.1 `application.yaml` (reemplaza `application.properties`)

```yaml
spring:
  application:
    name: urbanmovil-admin
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate        # el esquema lo maneja Flyway, nunca Hibernate
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
  docker:
    compose:
      lifecycle-management: start-only   # no apagar la BD al detener la app

server:
  servlet:
    session:
      cookie:
        http-only: true
        same-site: lax
      timeout: 8h

springdoc:
  swagger-ui:
    path: /swagger-ui.html

urbanmovil:
  zona-horaria: America/Bogota
  admin-inicial:
    nombre: ${ADMIN_NOMBRE:Administrador}
    correo: ${ADMIN_CORREO:admin@urbanmovil.co}
    contrasena: ${ADMIN_CONTRASENA:Admin12345}
  cors:
    origenes: ${CORS_ORIGENES:http://localhost:5173}
  demo:
    habilitado: false            # true en el perfil demo (Fase 3 usa esto)
```

Crea además `application-demo.yaml` con `urbanmovil.demo.habilitado: true`. El README debe explicar cómo arrancar con el perfil `demo` (`./gradlew bootRun --args='--spring.profiles.active=demo'`).

Usa una clase `@ConfigurationProperties(prefix = "urbanmovil")` para leer estas propiedades.

### 4.2 `compose.yaml`

Debe existir en la raíz (Initializr lo genera). Ajústalo para que quede así en esencia: servicio `postgres` con imagen oficial de PostgreSQL (versión estable actual), base `urbanmovil`, usuario `urbanmovil`, contraseña `urbanmovil`, puerto `5432` y un volumen con nombre para que los datos sobrevivan reinicios. Spring Boot lo levanta solo al ejecutar `bootRun` y configura la conexión automáticamente.

## 5. Base de datos (Flyway)

Crea **todo el esquema en `V1__esquema_inicial.sql`** (también las tablas de las fases 2 y 3), según el modelo de datos aprobado en `UrbanMovil_modelo_datos.drawio`. Así ninguna fase posterior necesita migraciones de estructura, salvo correcciones.

| Tabla | Columnas | Restricciones |
| --- | --- | --- |
| `administrador` | `id`, `nombre`, `correo`, `hash_contrasena`, `creado_en` | `correo` único (índice sobre `lower(correo)`) |
| `conductor` | `id`, `nombre`, `identificacion`, `correo`, `telefono`, `hash_contrasena`, `disponible` (bool, default true), `motivo_no_disponible` (texto, nullable), `detalle_motivo` (nullable), `creado_en`, `actualizado_en` | `identificacion` única; `correo` único (lower); `CHECK (motivo_no_disponible IN ('INCAPACIDAD','VACACIONES','PERMISO','OTRO'))`; `CHECK (disponible OR motivo_no_disponible IS NOT NULL)` |
| `vehiculo` | `id`, `placa`, `numero_interno`, `activo` (bool, default true), `motivo_inactivacion` (nullable), `fecha_inactivacion` (timestamptz, nullable), `creado_en`, `actualizado_en` | `placa` única (lower); `numero_interno` único (lower); `CHECK (activo OR motivo_inactivacion IS NOT NULL)` |
| `ruta` | `id`, `codigo`, `nombre`, `color`, `recorrido_principal` (jsonb), `creado_en`, `actualizado_en` | `codigo` único (lower); `nombre` único (lower) |
| `camino_alterno` | `id`, `ruta_id`, `nombre`, `puntos` (jsonb) | FK a `ruta` `ON DELETE CASCADE` |
| `programacion` | `id`, `conductor_id`, `fecha` (date) | FK a `conductor` `ON DELETE CASCADE`; único `(conductor_id, fecha)` |
| `programacion_ruta` | `id`, `programacion_id`, `ruta_id`, `orden` (int), `retiro_pendiente` (bool, default false) | FK a `programacion` y a `ruta`, ambas `ON DELETE CASCADE`; único `(programacion_id, ruta_id)` |
| `reasignacion` | `id`, `conductor_id`, `administrador_id`, `fecha_hora`, `motivo` | FK a `conductor` `ON DELETE CASCADE`; FK a `administrador` |
| `reasignacion_ruta` | `id`, `reasignacion_id`, `ruta_id`, `tipo` | FK a `reasignacion` y a `ruta`, `ON DELETE CASCADE`; `CHECK (tipo IN ('ASIGNADA','QUITADA'))` |
| `recorrido` | `id`, `conductor_id`, `ruta_id`, `vehiculo_id`, `estado`, `inicio`, `fin` (nullable) | FKs `ON DELETE CASCADE`; `CHECK (estado IN ('ACTIVO','FINALIZADO'))`; índice único parcial: un solo `ACTIVO` por conductor y un solo `ACTIVO` por vehículo |

Notas:

- `ON DELETE CASCADE` implementa la **decisión provisional D-17**: al eliminar definitivamente una ruta, un conductor o un vehículo (que no tenga recorrido activo), se borra también su historial asociado. Simplifica el MVP; queda pendiente de confirmación del equipo.
- Los trazados (`recorrido_principal`, `puntos`) son un arreglo JSON de puntos `[{"lat": 2.4419, "lng": -76.6066}, ...]`, en el sentido de circulación. Se mapean con `@JdbcTypeCode(SqlTypes.JSON)` a `List<Punto>`.
- Los enums se guardan como texto (`@Enumerated(EnumType.STRING)`).

## 6. Seguridad y login del administrador (HU-01)

**HU-01** — Como administrador de la empresa, quiero iniciar sesión en la aplicación web con mis credenciales, para acceder solo a las funciones de administración.

### 6.1 Diseño (simplificado para MVP académico)

- Sesión de servidor con cookie `JSESSIONID` (HttpOnly, SameSite=Lax). **Sin JWT**.
- Contraseñas con `BCryptPasswordEncoder`.
- Solo la tabla `administrador` puede autenticarse en la web. Un conductor no puede (CA-01.3) aunque exista su correo.
- CSRF **deshabilitado** para `/api/**` (simplificación documentada del MVP; la cookie SameSite=Lax y el CORS restringido mitigan el riesgo). Anótalo en el README.
- CORS: solo los orígenes de `urbanmovil.cors.origenes`, con `allowCredentials = true`.
- Rutas públicas: `POST /api/v1/auth/login`, `GET /api/v1/public/**`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`. Todo lo demás de `/api/v1/**` requiere rol `ADMIN`.
- Sin sesión, la API responde **401 JSON** (no redirige a una página de login ni muestra el formulario por defecto de Spring).
- Swagger debe permitir probar todo: tras llamar a `POST /auth/login` desde Swagger, la cookie queda en el navegador y los demás endpoints funcionan.

### 6.2 Administrador inicial (D-04, aprobada)

Al arrancar, si la tabla `administrador` está vacía, un `ApplicationRunner` crea uno con los datos de `urbanmovil.admin-inicial` (contraseña cifrada con BCrypt). No hay pantalla ni endpoint de registro de administradores.

### 6.3 Endpoints

| Método y ruta | Entrada | Salida | Notas |
| --- | --- | --- | --- |
| `POST /api/v1/auth/login` | `{ "correo", "contrasena" }` | 200 `{ id, nombre, correo }` + cookie de sesión | 401 `CREDENCIALES_INVALIDAS` con mensaje «Credenciales inválidas», igual si el correo no existe o la contraseña está mal |
| `POST /api/v1/auth/logout` | — | 204 | Invalida la sesión |
| `GET /api/v1/auth/me` | — | 200 `{ id, nombre, correo }` | 401 si no hay sesión |

### 6.4 Criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-01.1 | Un administrador registrado | Hace login con credenciales válidas | 200 y puede llamar a los endpoints protegidos |
| CA-01.2 | Contraseña incorrecta | Hace login | 401 «Credenciales inválidas», sin indicar qué dato falló |
| CA-01.3 | Un conductor registrado | Intenta hacer login con su correo y contraseña | 401 «Credenciales inválidas»; no obtiene sesión |

## 7. Conductores (HU-06, HU-07, HU-08)

**HU-06** — Como administrador de la empresa, quiero registrar un conductor con sus datos básicos y credenciales de acceso, para que pueda usar la aplicación del conductor.
**HU-07** — Como administrador de la empresa, quiero modificar los datos de un conductor o eliminarlo, para mantener actualizado el personal habilitado.
**HU-08** — Como administrador de la empresa, quiero marcar a un conductor como disponible o no disponible e indicar el motivo, para que solo los conductores disponibles puedan operar.

### 7.1 Datos y validaciones

| Campo | Regla |
| --- | --- |
| `nombre` | Obligatorio, máx. 120 |
| `identificacion` | Obligatorio, máx. 20, único |
| `correo` | Obligatorio, formato de correo, máx. 120, único |
| `telefono` | Obligatorio, máx. 20 |
| `contrasena` | Obligatoria al crear, mínimo 8 caracteres (**provisional**, ligado a D-07); opcional al modificar (si llega, se reemplaza); nunca se devuelve |

### 7.2 Endpoints

| Método y ruta | Descripción |
| --- | --- |
| `GET /api/v1/conductores?q=&disponible=` | Lista. `q` busca en nombre, identificación o correo. Cada elemento: `id, nombre, identificacion, correo, telefono, disponible, motivoNoDisponible, detalleMotivo, enRecorrido` (`null` o `{ recorridoId, rutaCodigo }`), `rutasHoy` (lista vacía hasta la Fase 3) |
| `GET /api/v1/conductores/{id}` | Detalle (mismos campos) |
| `POST /api/v1/conductores` | Registrar; 201 con el conductor creado |
| `PUT /api/v1/conductores/{id}` | Modificar datos (no cambia la disponibilidad) |
| `DELETE /api/v1/conductores/{id}` | Eliminar definitivamente (DP-09); 204 |
| `PATCH /api/v1/conductores/{id}/disponibilidad` | `{ "disponible": false, "motivo": "VACACIONES", "detalle": "Del 25 nov al 9 dic" }` |

### 7.3 Reglas y criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-06.1 | Datos completos y correo no registrado | Se registra el conductor | 201; queda `disponible = true` |
| CA-06.2 | Correo o identificación ya registrados | Se intenta registrar (o modificar a ese valor) | 409 `DATO_REPETIDO`, indicando cuál en `campos` |
| CA-06.3 | Un campo obligatorio vacío | Se intenta guardar | 400 `VALIDACION` señalando el campo |
| CA-07.1 | Conductor sin recorrido activo | Se modifican sus datos | 200 con los cambios |
| CA-07.2 | Conductor sin recorrido activo | Se elimina | 204; ya no existe (D-17: se borra su programación, reasignaciones y recorridos finalizados) |
| CA-07.3 | Conductor con recorrido `ACTIVO` | Se intenta eliminar | 409 `REGLA_NEGOCIO` «No se puede eliminar: el conductor tiene un recorrido activo. Debe finalizarlo primero.» (D-05) |
| CA-08.1 | Conductor disponible sin recorrido activo | Se marca no disponible con motivo `VACACIONES` | 200; quedan estado y motivo |
| CA-08.2 | Conductor disponible | Se marca no disponible sin motivo | 400 `VALIDACION` «Seleccione el motivo» |
| CA-08.3 | Conductor no disponible | Se marca disponible | 200; motivo y detalle quedan en `null` |
| CA-08.4 | Conductor con recorrido `ACTIVO` | Se intenta marcar no disponible | 409 `REGLA_NEGOCIO` «No se puede marcar como no disponible mientras tiene un recorrido activo.» (D-08, provisional) |
| V-6 (provisional) | Motivo `OTRO` | Se guarda sin `detalle` | 400 `VALIDACION` «Describa el motivo» |

«Tiene recorrido activo» = existe una fila en `recorrido` con ese `conductor_id` y `estado = 'ACTIVO'`. En esta fase no hay endpoint para crear recorridos (llega en la Fase 3); las pruebas los insertan directamente en la BD.

## 8. Vehículos (HU-28, HU-29, HU-30)

**HU-28** — Como administrador de la empresa, quiero registrar un vehículo de la empresa con su placa y su número interno, para saber qué buses pueden salir a operar e identificarlos en los recorridos y en el monitoreo.
**HU-29** — Como administrador de la empresa, quiero modificar los datos de un vehículo o eliminarlo, para mantener actualizado el registro de buses de la empresa.
**HU-30** — Como administrador de la empresa, quiero desactivar un vehículo escribiendo el motivo y reactivarlo cuando vuelva a estar en servicio, para que no se use un bus que no puede operar y quede constancia de por qué se retiró.

### 8.1 Datos y validaciones

| Campo | Regla |
| --- | --- |
| `placa` | Obligatoria, máx. 10. Se guarda en mayúsculas y sin espacios ni guiones (`abc-123` → `ABC123`). Única |
| `numeroInterno` | Obligatorio, máx. 20 (p. ej. `UM-104`). Se guarda en mayúsculas. Único |
| `motivoInactivacion` | Texto libre obligatorio al desactivar, máx. 300 (**D-15, provisional**) |

Solo placa y número interno (**D-14, provisional**). No agregues capacidad, modelo ni otros campos.

### 8.2 Endpoints

| Método y ruta | Descripción |
| --- | --- |
| `GET /api/v1/vehiculos?q=&activo=` | Lista: `id, placa, numeroInterno, activo, motivoInactivacion, fechaInactivacion, enRecorrido` |
| `GET /api/v1/vehiculos/{id}` | Detalle |
| `POST /api/v1/vehiculos` | Registrar; 201 |
| `PUT /api/v1/vehiculos/{id}` | Modificar placa y número interno |
| `DELETE /api/v1/vehiculos/{id}` | Eliminar definitivamente; 204 |
| `PATCH /api/v1/vehiculos/{id}/estado` | `{ "activo": false, "motivo": "Mantenimiento de frenos" }` o `{ "activo": true }` |

### 8.3 Reglas y criterios

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-28.1 | Placa y número interno no registrados | Se registra | 201; `activo = true` |
| CA-28.2 | Placa o número interno ya registrados | Se intenta registrar | 409 `DATO_REPETIDO` indicando cuál |
| CA-28.3 | Campo obligatorio vacío | Se intenta guardar | 400 `VALIDACION` |
| CA-29.1 | Vehículo registrado | Se cambian placa o número interno sin repetir otros | 200 |
| CA-29.2 | Vehículo sin recorrido activo | Se elimina | 204 |
| CA-29.3 | Vehículo con recorrido `ACTIVO` | Se intenta eliminar | 409 `REGLA_NEGOCIO` «No se puede eliminar: el vehículo tiene un recorrido activo. Debe finalizarse primero.» (RN-26) |
| CA-29.4 | — | — | Confirmación de eliminación: responsabilidad del front, no aplica al backend |
| CA-30.1 | Vehículo activo sin recorrido activo | Se desactiva con motivo | 200; `activo = false`, motivo y `fechaInactivacion = ahora` |
| CA-30.2 | Vehículo activo | Se desactiva sin motivo | 400 `VALIDACION` «Escriba el motivo de la desactivación» |
| CA-30.3 | Vehículo con recorrido `ACTIVO` | Se intenta desactivar | 409 `REGLA_NEGOCIO` «No se puede desactivar mientras tiene un recorrido activo.» (RN-26) |
| CA-30.4 | Vehículo inactivo | Se reactiva | 200; `activo = true`, motivo y fecha en `null` |

RN-25 (un vehículo inactivo no puede usarse en un recorrido) se aplica en la Fase 3, al crear recorridos de demostración.

## 9. Datos semilla (perfil `demo`)

Con `--spring.profiles.active=demo` y si las tablas están vacías, un `ApplicationRunner` del paquete `demo/` crea:

- 4 conductores (todos con contraseña `Conductor123`): 3 disponibles y 1 no disponible por `VACACIONES`.
- 5 vehículos con números internos `UM-104`, `UM-118`, `UM-221`, `UM-305`, `UM-410`: 4 activos y 1 inactivo con motivo «Mantenimiento de frenos».

Usa nombres ficticios. Las fases 2 y 3 añaden rutas, programación y recorridos a este mismo sembrador. Sin el perfil `demo` la base arranca vacía (salvo el administrador inicial).

## 10. Pruebas

- **Integración** con `@SpringBootTest` + Testcontainers PostgreSQL (`@ServiceConnection`), usando MockMvc y `spring-security-test` (`@WithMockUser(roles = ["ADMIN"])` o login real).
- Como mínimo, una prueba por cada CA de las tablas de §6.4, §7.3 y §8.3 (salvo CA-29.4), verificando código HTTP, `codigo` y `mensaje`.
- Una prueba que confirme que, sin sesión, `GET /api/v1/conductores` responde 401 JSON.
- Las pruebas deben pasar con `./gradlew test` teniendo Docker encendido.

## 11. Definición de terminado (Fase 1)

- [ ] `./gradlew bootRun` levanta PostgreSQL vía Docker Compose, aplica `V1` y arranca sin errores.
- [ ] Esquema completo de las 10 tablas creado por Flyway; Hibernate en `validate` no reporta diferencias.
- [ ] Administrador inicial creado al primer arranque.
- [ ] Login, logout y `me` funcionan; endpoints protegidos devuelven 401 JSON sin sesión.
- [ ] CRUD de conductores + disponibilidad y CRUD de vehículos + estado con todas las reglas de §7 y §8.
- [ ] Errores con el formato de §3.4 y mensajes en español.
- [ ] Swagger en `/swagger-ui.html` documenta y permite probar todos los endpoints.
- [ ] Perfil `demo` siembra conductores y vehículos.
- [ ] `./gradlew test` en verde.
- [ ] `README.md`: requisitos (JDK 21, Docker), cómo arrancar normal y con `demo`, credenciales por defecto, cómo correr pruebas, y las simplificaciones del MVP (CSRF, sesión, D-14, D-15, D-17).

## 12. Trazabilidad

| Elemento | HU | Reglas / decisiones |
| --- | --- | --- |
| Login y sesión | HU-01 | RNF-07, D-04 |
| Conductores | HU-06, HU-07 | D-05, D-09, DP-09, D-17 |
| Disponibilidad | HU-08 | RN-06, RN-13, D-08 |
| Vehículos | HU-28, HU-29, HU-30 | RN-25, RN-26, D-14, D-15, D-17 |
