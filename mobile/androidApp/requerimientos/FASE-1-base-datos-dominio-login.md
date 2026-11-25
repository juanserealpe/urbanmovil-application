# UrbanMovil · MVP Pasajero · Fase 1 de 3 — Base del proyecto, datos quemados, reglas de negocio y login

> Documento de implementación para el agente de desarrollo. Implementar **solo esta fase**. Las fases 2 y 3 vienen en archivos aparte y se apoyan en lo que se construya aquí.

## 1. Contexto

UrbanMovil (Urban Mobility Solutions) es una app para pasajeros del transporte público urbano de Popayán, Colombia. El pasajero consulta rutas, ve los buses moverse en el mapa, sabe en cuánto pasa su bus y qué ruta le sirve entre dos puntos.

Este MVP es un **prototipo de demostración**:

- Solo la épica del pasajero (EP-03). No hay app del conductor ni panel web.
- **Sin backend, sin API propia, sin base de datos.** Todos los datos (rutas, buses, conductores, lugares, usuario) están quemados en el código.
- Los buses se **simulan a partir de la hora actual del teléfono**, de modo que todos los teléfonos que abran la app al mismo tiempo ven los buses en la misma posición, sin servidor (ver §6).
- Se prueba en un teléfono Android real.
- Se construye con **Kotlin Multiplatform + Compose Multiplatform** (solo target Android por ahora) para poder sumar iOS más adelante reutilizando la lógica y la interfaz.

## 2. Objetivo de esta fase

Dejar el proyecto Kotlin Multiplatform (solo target Android) compilando e instalando en el teléfono, con:

1. Estructura de carpetas, tema visual y navegación con barra inferior (pantallas vacías de momento).
2. Modelos y datos quemados.
3. Repositorio (`TransitRepository`) con su implementación falsa y el simulador de buses por hora.
4. Reglas de negocio puras (`domain/`) con pruebas unitarias: vigencia de ubicación, geometría, tiempo de llegada y búsqueda origen–destino.
5. Inicio de sesión quemado (HU-17) y pantalla de Perfil con cerrar sesión.

## 3. Stack obligatorio

El proyecto **ya fue creado** con el asistente de Kotlin Multiplatform de Android Studio con estas opciones: target **solo Android**, **Share UI** (Compose Multiplatform), paquete `co.urbanmovil.app`. Trabaja sobre ese proyecto; no lo regeneres ni lo conviertas en un proyecto Android tradicional.

| Capa | Elección |
| --- | --- |
| Tipo de proyecto | Kotlin Multiplatform, estructura nueva: módulo `shared` (librería KMP) + módulo `androidApp` (punto de entrada Android) |
| Lenguaje | Kotlin |
| Build | Gradle con version catalog (`gradle/libs.versions.toml`), tal como lo generó el asistente |
| UI | Compose Multiplatform + Material 3, en `shared/src/commonMain` |
| Navegación | Navigation Compose multiplatform (`org.jetbrains.androidx.navigation:navigation-compose`) |
| Estado | ViewModel multiplatform (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`) + `StateFlow` |
| Concurrencia | `kotlinx-coroutines-core` (Flow) |
| Hora | `kotlin.time.Clock` para la hora actual; `kotlinx-datetime` para zona horaria `America/Bogota` |
| Recursos | Compose Multiplatform resources (`shared/src/commonMain/composeResources/`) para fuentes, textos e íconos |
| Pruebas | `kotlin.test` en `shared/src/commonTest` |
| minSdk | El que generó el asistente, no menor de 26 (Android 8.0) |
| Paquete | `co.urbanmovil.app` |

Usa las versiones estables más recientes compatibles con las de Kotlin y Compose Multiplatform del proyecto; no fijes versiones antiguas copiadas de ejemplos.

**Regla de código común:** en `commonMain` y `commonTest` **no** se puede importar `android.*` ni `java.*` (nada de `System.currentTimeMillis()`, `java.time`, `java.util.UUID`, `String.format`, JUnit). Si algo necesita Android, se declara con `expect` en `commonMain` y se implementa con `actual` en `shared/src/androidMain`.

**Prohibido en todo el MVP:** Retrofit/Ktor/llamadas HTTP propias, Room/SQLite, Firebase, Supabase, Hilt/Koin (la inyección es manual), login con Google, código de confirmación por correo. La única conexión a internet permitida es la que hace la librería de mapa para descargar teselas (Fase 2).

## 4. Estructura de carpetas

```
shared/
├─ build.gradle.kts
└─ src/
   ├─ commonMain/
   │  ├─ composeResources/          // font/, drawable/, values/strings.xml
   │  └─ kotlin/co/urbanmovil/app/
   │     ├─ App.kt                  // @Composable App(esDebug: Boolean): tema + navegación
   │     ├─ AppContainer.kt         // crea y expone el repositorio (inyección manual)
   │     ├─ data/
   │     │  ├─ model/               // Punto, Ruta, Bus, EstadoBus, Lugar, NivelOcupacion, Vigencia
   │     │  ├─ TransitRepository.kt // interfaz
   │     │  ├─ FakeTransitRepository.kt
   │     │  ├─ FakeData.kt          // rutas, buses, lugares, usuario demo, constantes
   │     │  └─ BusSimulator.kt      // posición de cada bus en función de la hora
   │     ├─ domain/
   │     │  ├─ Geo.kt               // haversine, punto más cercano, distancia sobre recorrido
   │     │  ├─ Freshness.kt         // RN-07
   │     │  ├─ EtaCalculator.kt     // HU-20 / HU-22
   │     │  └─ RouteSearch.kt       // HU-21
   │     └─ ui/
   │        ├─ theme/
   │        ├─ navigation/          // NavHost + barra inferior
   │        ├─ login/  profile/
   │        ├─ routes/  routedetail/  map/   // Fase 2
   │        └─ eta/  search/  guide/         // Fase 3
   ├─ commonTest/kotlin/co/urbanmovil/app/domain/   // pruebas kotlin.test
   └─ androidMain/kotlin/co/urbanmovil/app/         // solo `actual` si hacen falta
androidApp/
└─ src/main/
   ├─ AndroidManifest.xml
   ├─ kotlin/co/urbanmovil/app/MainActivity.kt   // setContent { App(esDebug = BuildConfig.DEBUG) }
   └─ res/                                       // ícono de la app
```

Si el asistente generó nombres de archivo distintos (p. ej. `App.kt` de ejemplo, `Greeting.kt`, `Platform.kt`), reemplaza o elimina el código de ejemplo y conserva la configuración de Gradle.

Reglas de arquitectura:

- **Las pantallas y ViewModels nunca leen `FakeData` directamente**; siempre pasan por `TransitRepository`. Así, en el futuro se reemplaza `FakeTransitRepository` por una implementación real sin tocar la UI.
- `androidApp` solo contiene `MainActivity`, manifiesto, ícono y configuración de la app. Toda la lógica y la UI viven en `shared`.
- `BuildConfig.DEBUG` solo existe en `androidApp`; se pasa a la UI compartida como parámetro `esDebug`.

## 5. Modelos y datos quemados

### 5.1 Modelos (orientativos; el agente puede ajustar nombres internos)

```kotlin
data class Punto(val lat: Double, val lng: Double)

data class Ruta(
    val id: String,              // "R1"
    val nombre: String,          // "Centro – Campanario"
    val colorHex: Long,          // color de la ruta en el mapa
    val principal: List<Punto>,  // ordenado en el sentido de circulación, del inicio al final
    val alternos: List<List<Punto>>, // informativos: NUNCA se usan para ETA ni búsqueda (RN-03)
    val minutosPorVuelta: Int    // duración de un recorrido completo para la simulación
)

enum class NivelOcupacion { CON_PUESTOS, SOLO_DE_PIE, LLENO }

data class Bus(
    val id: String,              // "UM-104"
    val rutaId: String,
    val conductor: String,       // solo informativo
    val desfaseMin: Int,         // separa los buses de una misma ruta
    val ventanaSinSenalMin: IntRange? = null, // minutos de cada vuelta en que "pierde señal"
    val ocupacion: NivelOcupacion? = null,
    val ocupacionReportadaHaceMin: Int? = null
)

enum class Vigencia { RECIENTE, DESACTUALIZADA, OCULTA }

data class EstadoBus(
    val bus: Bus,
    val posicion: Punto,
    val indiceEnRecorrido: Double, // posición sobre el recorrido principal (fraccional)
    val horaUltimaUbicacion: Long, // epoch millis
    val vigencia: Vigencia
)

data class Lugar(val nombre: String, val punto: Punto)
```

### 5.2 Datos a quemar en `FakeData.kt`

- **3 rutas** sobre calles reales de Popayán, cada una con su recorrido principal de 15 a 40 puntos (suficientes para que la línea siga las calles):
  - R1 «Centro – Campanario», color `#1F4FD8`, **con 1 camino alterno**.
  - R2 «Bello Horizonte – Terminal», color `#5B6B88`.
  - R3 «La Esmeralda – Pomona», color `#7A5AC8`.
  - Referencia: el Parque Caldas está aproximadamente en `2.4419, -76.6066`. Los puntos de las rutas son ficticios; el equipo los puede reemplazar luego por rutas reales (decisión DP-11). Deja un comentario en el archivo indicando cómo trazarlos (p. ej. dibujar en geojson.io y copiar coordenadas).
  - Al menos un par de rutas deben pasar cerca del barrio Valencia y de la Universidad del Cauca (sede Tulcán) para que la búsqueda de la demo encuentre 2 opciones.
- **5 buses**: 2 en R1, 2 en R2, 1 en R3.
  - Uno de R1 (`UM-221`) con `ventanaSinSenalMin = 10..13` para mostrar «Desactualizado».
  - Un bus de R3 con `ventanaSinSenalMin` que supere 15 min (p. ej. `5..25`) para demostrar que el bus se oculta (CA-19.3) y que R3 aparece «sin buses activos» parte del tiempo.
  - Ocupación quemada en 3 de los 5 buses; uno con reporte de hace más de 10 min.
- **Lugares** para la búsqueda (mínimo 6): Barrio Valencia, Universidad del Cauca – Sede Tulcán, Parque Caldas, Terminal de Transportes, Centro Comercial Campanario, Hospital San José. Coordenadas aproximadas.
- **«Mi ubicación»**: punto fijo quemado en el barrio Valencia (no se usa GPS en el MVP).
- **Usuario demo**: `pasajero@demo.com` / `123456`.
- **Constantes configurables**:
  - `VELOCIDAD_PROMEDIO_KMH = 18.0` (pendiente de confirmar por el equipo; debe poder cambiarse en un solo lugar).
  - `MAX_CAMINATA_M = 500.0` (RN-08).
  - `VIGENCIA_RECIENTE_S = 60`, `VIGENCIA_OCULTA_MIN = 15` (RN-07).
  - `OCUPACION_VIGENTE_MIN = 10` (CA-24.2).
  - `MOSTRAR_OCUPACION = true` (HU-24 es «Should» y está pendiente de aprobación, D-01; debe poder apagarse con esta bandera).

## 6. Simulación de buses sincronizada por hora (sin servidor)

La posición de cada bus **no** se calcula con un contador interno, sino a partir de la hora actual. Así todos los teléfonos ven lo mismo y reabrir la app no «reinicia» los buses.

```kotlin
fun progresoEnVuelta(bus: Bus, ruta: Ruta, ahoraMs: Long): Double {
    val vueltaMs = ruta.minutosPorVuelta * 60_000L
    val t = (ahoraMs + bus.desfaseMin * 60_000L).mod(vueltaMs)
    return t.toDouble() / vueltaMs          // 0.0 = punto de inicio, 1.0 = punto final
}
```

Reglas:

1. El progreso (0..1) se convierte en una posición sobre el recorrido principal **proporcional a la distancia**, no al número de puntos (interpolar entre puntos).
2. Al llegar al final la vuelta vuelve a empezar en el punto de inicio.
3. Si el minuto actual de la vuelta cae dentro de `ventanaSinSenalMin`, el bus queda **congelado** en la posición del inicio de la ventana y su `horaUltimaUbicacion` es la hora de inicio de la ventana. Fuera de la ventana, `horaUltimaUbicacion = ahora`.
4. La vigencia se calcula con `Freshness` (§7.2).
5. `TransitRepository` expone un `Flow<List<EstadoBus>>` que emite cada **1 segundo**.
6. `BusSimulator` recibe la hora como parámetro (`ahoraMs`) para poder probarlo sin esperar. En producción la hora sale de `Clock.System.now().toEpochMilliseconds()` (`kotlin.time.Clock`), nunca de `System.currentTimeMillis()`.
7. El repositorio admite un **desfase de reloj** (`ajusteRelojMs`, 0 por defecto) que la Fase 2 usará para el control debug de «adelantar el tiempo».

## 7. Reglas de negocio (`domain/`) — funciones puras con pruebas

### 7.1 Geo

- `distanciaM(a, b)`: haversine en metros.
- `puntoMasCercano(p, polilinea)`: proyecta `p` sobre cada segmento y devuelve el punto proyectado, su distancia a `p` y su posición acumulada en metros desde el inicio del recorrido.
- `distanciaSobreRecorrido(desdeM, hastaM)`: diferencia de posiciones acumuladas.
- Las distancias a pie son en **línea recta** (no por calles). Es una simplificación aceptada para la demo.

### 7.2 Vigencia (RN-07, HU-19)

| Edad de la última ubicación | Vigencia |
| --- | --- |
| ≤ 60 s | RECIENTE |
| > 60 s y ≤ 15 min | DESACTUALIZADA |
| > 15 min | OCULTA (no se muestra) |

### 7.3 Tiempo de llegada (HU-20, HU-22)

Entrada: un punto P y una ruta. Salida: uno de

- `LejosDeLaRuta` — el punto del recorrido principal más cercano a P está a más de 500 m (CA-20.4).
- `SinBusProximo` — no hay ningún bus RECIENTE de esa ruta que aún no haya pasado por el punto del recorrido más cercano a P (RN-11, CA-20.3). Un bus DESACTUALIZADO u OCULTO **no** cuenta.
- `Llega(minutos, puntoDelRecorrido)` — minutos = distancia sobre el recorrido desde el bus más cercano aguas arriba hasta ese punto ÷ `VELOCIDAD_PROMEDIO_KMH`, redondeado hacia arriba, mínimo 1.

`tiempoTrayecto(abordajeM, descensoM)` = distancia sobre el recorrido ÷ velocidad promedio, en minutos.

### 7.4 Búsqueda origen–destino (HU-21)

Por cada ruta:

1. Abordaje = punto del recorrido principal más cercano al origen; descenso = el más cercano al destino.
2. Se **descarta** si la caminata al abordaje o desde el descenso supera 500 m (RN-08) o si el descenso queda **antes** del abordaje en el sentido de circulación (CA-21.3).
3. Sin transbordos (RN-10). Solo recorrido principal, nunca alternos (RN-03).

Resultado: lista ordenada por menor caminata al abordaje; en empate, por menor caminata desde el descenso (CA-21.2). Cada opción incluye: ruta, punto de abordaje, punto de descenso, metros a pie de cada tramo, resultado de ETA al abordaje y tiempo de trayecto. Los tiempos **no** alteran el orden (DP-15).

Errores: origen igual (o a menos de 20 m) del destino → `PuntosIguales` (CA-21.5); ninguna ruta → lista vacía.

### 7.5 Pruebas unitarias mínimas

- Vigencia: 0 s, 60 s, 61 s, 15 min, 15 min + 1 s.
- Simulador: dos llamadas con la misma `ahoraMs` devuelven la misma posición; dentro de la ventana sin señal el bus no se mueve y su edad crece.
- ETA: punto a > 500 m → `LejosDeLaRuta`; bus que ya pasó → `SinBusProximo`; bus desactualizado → `SinBusProximo`; caso feliz devuelve minutos > 0.
- Búsqueda: ruta en sentido contrario no aparece; orden por caminata con desempate; puntos iguales → error; los alternos no se usan.

Las pruebas se escriben con `kotlin.test` en `shared/src/commonTest` y deben correr en la máquina de desarrollo (host), sin emulador. Si el plugin de librería Android-KMP del módulo `shared` no tiene habilitadas las pruebas de host, habilítalas según su documentación. Documenta en el README la tarea Gradle exacta para ejecutarlas (p. ej. `./gradlew :shared:allTests` o la tarea de pruebas de host que exponga el plugin).

## 8. Inicio de sesión quemado (HU-17) y Perfil

**HU-17** — Como pasajero, quiero iniciar sesión con mi correo y contraseña, para volver a usar la aplicación con mi cuenta.

Pantalla según el mockup «01 · Inicio de sesión»: logo, nombre «UrbanMovil», campos Correo y Contraseña, botón «Ingresar», botón «Crear cuenta» y un aviso «Prototipo: el acceso es de demostración».

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-17.1 | El usuario demo | Ingresa `pasajero@demo.com` / `123456` | Entra a la pestaña Rutas |
| CA-17.2 | Una contraseña incorrecta | Intenta ingresar | Se muestra «Credenciales inválidas» sin indicar qué dato falló y no entra |
| CA-17.3 (MVP) | La pantalla de acceso | Toca «Crear cuenta» | Entra directo como usuario demo (HU-15 queda fuera del MVP) |
| CA-17.4 (MVP) | Sesión iniciada | Abre Perfil y toca «Cerrar sesión» | Vuelve a la pantalla de acceso |

- Los campos pueden venir precargados con las credenciales demo para agilizar la demo.
- La sesión vive en memoria (no hace falta persistirla).
- Sin sesión no se puede navegar a ninguna otra pantalla (RN-18).

## 9. Navegación y tema

- Barra inferior con 4 destinos: **Rutas**, **Cuándo pasa**, **Buscar viaje**, **Perfil**. En esta fase Rutas, Cuándo pasa y Buscar viaje muestran un texto provisional.
- Rutas adicionales sin barra: `detalleRuta/{rutaId}` y `guia/{opcionId}` (se llenan en fases 2 y 3).
- Tema Material 3 claro con estos colores de los mockups:

| Uso | Color |
| --- | --- |
| Primario / R1 / recorrido principal | `#1F4FD8` |
| Contenedor primario (pastillas, chips) | `#E4EBFC` |
| Camino alterno | `#C25E00` |
| Texto principal | `#12161C` |
| Texto secundario | `#4B5563` |
| Fondo | `#F6F7F9` |
| Bordes | `#E3E6EB` |
| Éxito / «Ubicación reciente» | texto `#0E5A2F` sobre `#DDF3E5` |
| Desactualizado | `#374151` / `#6B7280` |
| Aviso (ocupación, modo demo) | texto `#7A3E00` sobre `#FFF4E5` |

- Tipografía: Inter para el cuerpo y Space Grotesk para los títulos, como archivos `.ttf` en `shared/src/commonMain/composeResources/font/` (ambas tienen licencia Open Font License). Si complica, usar la del sistema.
- Íconos: Material Icons de Compose Multiplatform o vectores propios en `composeResources/drawable/`.
- Áreas táctiles de 48 dp como mínimo; `contentDescription` en botones con solo ícono.

## 10. Definición de terminado (Fase 1)

- [ ] El proyecto compila sin advertencias graves y la configuración `androidApp` se instala en un teléfono Android 8.0 o superior.
- [ ] Ningún archivo de `commonMain` ni `commonTest` importa `android.*` ni `java.*`.
- [ ] El código de ejemplo del asistente fue retirado.
- [ ] Login quemado funciona según CA-17.1 a CA-17.4.
- [ ] La barra inferior navega entre las 4 pestañas.
- [ ] `FakeData.kt` contiene 3 rutas, 5 buses, 6 lugares y las constantes de §5.2.
- [ ] `TransitRepository` + `FakeTransitRepository` + `BusSimulator` por hora implementados.
- [ ] Todas las pruebas de §7.5 pasan en la máquina de desarrollo con la tarea documentada en el README.
- [ ] Ninguna pantalla importa `FakeData` directamente.
- [ ] README corto en la raíz: cómo abrir, compilar, correr pruebas e instalar.

## 11. Fuera del alcance de esta fase

Mapa (Fase 2), lista y detalle de rutas (Fase 2), «Cuándo pasa», búsqueda y guía (Fase 3).

## 12. Trazabilidad

| Elemento | HU / Regla |
| --- | --- |
| Login quemado | HU-17, RN-18 |
| Vigencia | RN-07, HU-19 |
| ETA | HU-20, HU-22, RN-03, RN-08, RN-11 |
| Búsqueda | HU-21, RN-04, RN-08, RN-09, RN-10 |
| Bandera de ocupación | HU-24, D-01 (pendiente) |
