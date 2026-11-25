# UrbanMovil · MVP Pasajero

Prototipo de demostración para pasajeros del transporte urbano de Popayán. Kotlin Multiplatform +
Compose Multiplatform (por ahora solo target Android). **Sin backend**: rutas, buses, lugares y usuario
están quemados en `shared/.../data/FakeData.kt`, y los buses se simulan a partir de la hora del teléfono
(todos los teléfonos ven lo mismo al mismo tiempo).

Usuario demo: `pasajero@demo.com` / `123456` (vienen precargados; «Crear cuenta» también entra como demo).

## Estructura

| Módulo / carpeta | Qué contiene |
| --- | --- |
| `shared/src/commonMain/.../data` | Modelos, `FakeData`, `TransitRepository` + `FakeTransitRepository`, `BusSimulator`, `AuthRepository` |
| `shared/src/commonMain/.../domain` | Reglas puras: `Geo`, `Freshness` (RN-07), `EtaCalculator` (HU-20/22), `RouteSearch` (HU-21) |
| `shared/src/commonMain/.../ui/map` | `MapaUrbano`: ÚNICO archivo que conoce MapLibre; el resto usa `LineaMapa`, `MarcadorMapa`, `CamaraMapa` |
| `shared/src/commonMain/.../ui/rutas` | Pestaña Rutas y Detalle de ruta (HU-18, HU-19, HU-24) |
| `shared/src/commonMain/.../ui/viaje` | ¿Cuándo pasa?, Buscar viaje y Guía (HU-20 a HU-23) |
| `shared/src/commonMain/.../ui` (resto) | Tema, navegación con barra inferior, login, perfil |
| `shared/src/commonTest` | Pruebas `kotlin.test` (corren en el host, sin emulador) |
| `androidApp` | Solo `MainActivity`, manifiesto e ícono |

Reglas: las pantallas y ViewModels nunca leen `FakeData`, siempre pasan por los repositorios
(inyección manual en `AppContainer`). En `commonMain`/`commonTest` no se importa `android.*` ni `java.*`.

## Abrir

Android Studio → *Open* → carpeta raíz del proyecto. Esperar el Gradle sync.

## Compilar

```bash
./gradlew :androidApp:assembleDebug
```

## Correr las pruebas (host, sin emulador)

```bash
./gradlew :shared:testAndroidHostTest
```

Las pruebas de `commonTest` se ejecutan a través del *host test* del plugin
`com.android.kotlin.multiplatform.library` (habilitado con `withHostTest {}` en `shared/build.gradle.kts`).
Reporte: `shared/build/reports/tests/`.

## Mapa

MapLibre Compose con el estilo gratuito de [OpenFreeMap](https://openfreemap.org) (sin API key, sin
registro, sin facturación). Sin internet las teselas no cargan, pero rutas y buses se siguen dibujando
sobre un fondo liso. La atribución de OpenStreetMap está en el botón de la esquina inferior del mapa.

## Instalar en el teléfono

Teléfono con Android 8.0 (API 26) o superior, con *Depuración USB* activada:

```bash
./gradlew :androidApp:installDebug
```

O desde Android Studio: configuración **androidApp** → ▶ Run.

## Datos configurables

En `FakeData.kt`: `VELOCIDAD_PROMEDIO_KMH`, `MAX_CAMINATA_M`, `VIGENCIA_RECIENTE_S`, `VIGENCIA_OCULTA_MIN`,
`OCUPACION_VIGENTE_MIN`, `MOSTRAR_OCUPACION`. Los recorridos son ficticios; el comentario al inicio del
archivo explica cómo trazar los reales con geojson.io.

## APK para la demo

```bash
./gradlew :androidApp:assembleDebug
```

El archivo queda en `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Para instalarlo:

- Con cable: `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`
  (`adb` está en `~/Android/Sdk/platform-tools/`).
- Sin cable: copiar el `.apk` al teléfono, abrirlo y permitir «instalar apps de origen desconocido».

**Para la demo se entrega el APK debug.** El release (`assembleRelease`) sale sin firmar y no se puede
instalar hasta configurar una clave en `signingConfigs`. Hoy el release tiene `isMinifyEnabled = false`,
así que R8 no recorta MapLibre; si algún día se activa, hay que volver a probar el mapa y la navegación
en un teléfono antes de entregarlo. El APK debug además trae el control para adelantar el reloj.

## Guion de demo (2–3 min)

1. **Iniciar sesión** con `pasajero@demo.com` / `123456`.
2. **Rutas → R1 Centro – Campanario**: el bus se mueve solo. `UM-221` sale «Desactualizado»
   (círculo blanco con borde punteado) entre los minutos 10 y 13 de cada vuelta. Para no esperar,
   **mantener presionado el título** de la pantalla: cada vez adelanta el reloj 3 min (solo en debug;
   tocar el aviso naranja lo reinicia).
3. **¿Cuándo pasa?**: con «Mi ubicación» se ve «~N min» por ruta. Activar «Punto en el mapa», tocar
   un punto lejos de todas las rutas y mostrar «Lejos de la ruta».
4. **Buscar viaje**: Mi ubicación (Valencia) → Universidad del Cauca – Sede Tulcán viene precargado.
   Comparar las 2 opciones: caminata, «Llega en ~N min» y «Trayecto ~M min».
5. Tocar la primera opción para abrir la **Guía**: tramo a pie punteado, tramo en bus sólido, pines
   A, B y D. «Ver otras opciones» vuelve a la lista.
6. Abrir la app en **un segundo teléfono**: los buses están en el mismo lugar (la posición sale de la
   hora; ambos teléfonos deben tener la hora automática).
