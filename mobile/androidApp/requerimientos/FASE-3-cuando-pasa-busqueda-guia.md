# UrbanMovil · MVP Pasajero · Fase 3 de 3 — ¿Cuándo pasa?, búsqueda de viaje, guía y entrega

> Documento de implementación para el agente de desarrollo. **Requiere las fases 1 y 2 terminadas** (`domain/` con ETA y búsqueda probados, `MapaUrbano`, rutas y buses en vivo). Implementar solo esta fase.

## 1. Contexto breve

App para pasajeros de buses urbanos de Popayán, hecha con **Kotlin Multiplatform + Compose Multiplatform** (solo target Android): la UI y la lógica viven en `shared/src/commonMain`, y `androidApp` solo tiene el `MainActivity`. Prototipo **sin backend**: rutas, buses y lugares quemados; los buses se mueven según la hora del teléfono. Las reglas de stack y prohibiciones de la Fase 1 siguen vigentes, incluida la de no importar `android.*` ni `java.*` en `commonMain`. Toda la lógica de tiempos y búsqueda **ya existe** en `domain/` (`EtaCalculator`, `RouteSearch`): esta fase la conecta con la UI y **no** debe reimplementarla en los ViewModels.

## 2. Objetivo de esta fase

1. Pestaña **Cuándo pasa** (HU-20).
2. Pestaña **Buscar viaje** con la lista de opciones y sus tiempos (HU-21, HU-22).
3. Pantalla **Guía** de la opción elegida (HU-23).
4. Pulido general y APK instalable para la demo.

## 3. Pestaña «¿Cuándo pasa?» — HU-20

**HU-20** — Como pasajero, quiero elegir mi ubicación o un punto del mapa y ver en cuánto tiempo pasará el próximo bus de una o varias rutas por el punto del recorrido más cercano, para decidir cuándo salir y cuánto esperaré.

Pantalla según mockup «04 · ¿Cuándo pasa?»:

- Barra superior «¿Cuándo pasa?» con subtítulo «Toque el mapa para mover el punto».
- Mapa con todas las rutas, los buses y un pin rojo «P» en el punto elegido. Para cada ruta a ≤ 500 m, un pequeño círculo en el punto del recorrido más cercano y una línea punteada fina entre P y ese punto.
- Dos botones sobre el mapa: «Mi ubicación» (usa el punto fijo quemado) y «Punto en el mapa» (activa el modo en que tocar el mapa mueve P). Estado inicial: P = Mi ubicación.
- Lista «Próximo bus por este punto» con la nota «Tiempos aproximados», una tarjeta por ruta:

| Resultado de `EtaCalculator` | Tarjeta |
| --- | --- |
| `Llega(min, punto)` | Número grande «~N min» + «Pasa por [punto] · a X m» |
| `SinBusProximo` | «Sin bus próximo» en texto secundario + motivo («El único bus activo ya pasó» o «No hay buses con ubicación reciente») |
| `LejosDeLaRuta` | «Lejos de la ruta» + «La ruta pasa a más de 500 m» |

- Orden de las tarjetas: primero las que tienen tiempo (de menor a mayor), luego «Sin bus próximo», luego «Lejos de la ruta».
- Los tiempos se recalculan cada segundo con el `Flow` de buses.
- Para nombrar el punto del recorrido basta con «Punto del recorrido a X m» si no hay un nombre quemado cercano; opcionalmente usar el `Lugar` quemado más cercano.

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-20.1 | Un punto a ≤ 500 m de una ruta y un bus reciente que aún no pasa | El pasajero consulta | Ve un tiempo etiquetado como aproximado y el punto del recorrido al que se refiere |
| CA-20.2 | Varias rutas cerca | El pasajero consulta | Ve un tiempo (o estado) por cada ruta |
| CA-20.3 | Una ruta sin bus reciente que no haya pasado | El pasajero consulta | Ve «Sin bus próximo» en lugar de un tiempo |
| CA-20.4 | Un punto a más de 500 m del recorrido | El pasajero consulta | Se informa que el punto está lejos de la ruta y no se calcula tiempo |
| CA-20.5 (MVP) | — | — | No aplica: «Mi ubicación» es un punto fijo, no se piden permisos de ubicación |

## 4. Pestaña «Buscar viaje» — HU-21 y HU-22

**HU-21** — Como pasajero, quiero marcar un origen y un destino y obtener la lista de rutas que me sirven, ordenada por menor caminata, para tomar la ruta que me deja más cerca caminando lo menos posible.

**HU-22** — Como pasajero, quiero ver en la descripción de cada opción cuánto tarda el próximo bus en llegar a mi punto de abordaje y cuánto tarda el trayecto hasta mi punto de descenso, para comparar rutas directas con rutas que dan muchas vueltas.

Pantalla según mockup «05 · Buscar viaje»:

- Barra superior «Buscar viaje» con subtítulo «Sin transbordos · máx. 500 m a pie».
- Campos **Origen** y **Destino**. Al tocarlos se abre una hoja con:
  - «Mi ubicación» (punto fijo),
  - la lista de `Lugar` quemados,
  - «Elegir en el mapa» (abre un mapa a pantalla completa; tocar fija el punto y vuelve).
- Botón circular para intercambiar origen y destino.
- Valores iniciales para la demo: Origen = Mi ubicación (barrio Valencia), Destino = Universidad del Cauca – Sede Tulcán. La búsqueda se ejecuta automáticamente al tener ambos puntos.
- Resultado: título «N rutas le sirven» y nota «Ordenadas por caminata». Una tarjeta por opción:
  - Insignia de la ruta + nombre.
  - «Camina X m al abordaje · Y m desde el descenso».
  - Chips: «Menos caminata» (solo la primera), «Llega en ~N min» (o «Sin bus próximo»), «Trayecto ~M min».
  - Tocar la tarjeta abre la guía.
- Nota al pie: «Los tiempos son aproximados y no cambian el orden de la lista».

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-21.1 | Origen y destino a ≤ 500 m de una ruta, con abordaje antes del descenso | El pasajero busca | La ruta aparece |
| CA-21.2 | Dos o más rutas posibles | Se muestra la lista | Orden por menor caminata al abordaje y, en empate, por menor caminata desde el descenso |
| CA-21.3 | Una ruta cercana a ambos puntos pero en sentido contrario | El pasajero busca | Esa ruta no aparece |
| CA-21.4 | Ninguna ruta posible | El pasajero busca | Se muestra «No hay rutas que conecten estos puntos» |
| CA-21.5 | Origen igual al destino | El pasajero busca | Se pide «Elija un origen y un destino distintos» |
| CA-21.6 (MVP) | — | — | No aplica: la búsqueda es local y no depende de conexión |
| CA-22.1 | Una opción con bus que cumple RN-11 | Se muestra la lista | Incluye el tiempo aproximado de llegada al abordaje y el de trayecto |
| CA-22.2 | Dos opciones, una con recorrido más largo entre abordaje y descenso | Se muestran | La de recorrido más largo tiene mayor tiempo de trayecto |
| CA-22.3 | Una opción sin bus próximo | Se muestra | Indica «Sin bus próximo» y conserva el tiempo de trayecto |
| CA-22.4 | Opciones con distintos tiempos | Se muestra la lista | El orden sigue siendo el de menor caminata |

Verifica que con los datos quemados el caso de la demo (Valencia → Universidad del Cauca) devuelva **2 opciones**; si no, ajusta las coordenadas de `FakeData` (no la lógica).

## 5. Pantalla «Guía» — HU-23

**HU-23** — Como pasajero, quiero ver en el mapa el tramo a pie hasta el abordaje, el tramo en bus, el punto de descenso y el tramo a pie hasta el destino, para saber exactamente dónde tomar y dónde dejar el bus.

Pantalla según mockup «06 · Guía del viaje»:

- Barra superior con flecha atrás, «Guía · R1» y subtítulo «Hacia [destino]».
- Mapa encuadrado al viaje completo:
  - Recorrido completo de la ruta tenue de fondo.
  - **Tramo en bus** (de abordaje a descenso, sobre el recorrido principal): línea sólida gruesa del color de la ruta.
  - **Tramos a pie** (origen → abordaje y descenso → destino): línea punteada fina oscura, en línea recta.
  - Marcadores: punto azul del origen, pin verde «A» (abordaje), pin rojo «B» (descenso), pin oscuro «D» (destino).
- Chips: «Bus llega en ~N min» (o «Sin bus próximo») y «Trayecto ~M min», actualizados en vivo.
- Pasos:
  1. «Camine X m hasta A» + nombre o descripción del punto.
  2. «Tome la R1 Centro – Campanario» + «Bájese en B».
  3. «Camine Y m hasta su destino» + nombre del destino.
- Botón «Ver otras opciones» que vuelve a la lista conservando la búsqueda.

| ID | Dado | Cuando | Entonces |
| --- | --- | --- | --- |
| CA-23.1 | Una opción elegida | El pasajero abre la guía | El mapa muestra con estilos distintos el tramo a pie al abordaje, el tramo en bus y el tramo a pie desde el descenso, y marca ambos puntos |
| CA-23.2 | La guía abierta | El pasajero vuelve a la lista | Puede elegir otra opción y ver su guía |

## 6. Pulido y entrega

- Estados vacíos y mensajes de §3 y §4 revisados con textos en español claro.
- Revisar en un teléfono pequeño (≈ 360 dp de ancho) que nada se corte.
- Accesibilidad: `contentDescription` en pines y botones de ícono; tarjetas leíbles con TalkBack como una unidad (`Modifier.semantics(mergeDescendants = true)`) («Ruta R1, llega en 6 minutos aproximadamente, trayecto 22 minutos»).
- Ícono de la app en `androidApp/src/main/res/` y nombre «UrbanMovil» (`android:label` en el manifiesto de `androidApp`).
- Generar **APK** instalable desde el módulo de la app: `./gradlew :androidApp:assembleDebug` (o un release firmado con una clave de prueba: `./gradlew :androidApp:assembleRelease`). Documentar en el README dónde queda el archivo (`androidApp/build/outputs/apk/...`) y cómo instalarlo (`adb install` o copiándolo al teléfono).
- Si se genera release, verificar que R8/ProGuard no rompa MapLibre ni la serialización de navegación; si lo hace, entregar el APK debug para la demo y anotarlo en el README.
- Añadir al README un **guion de demo** de 2–3 minutos:
  1. Iniciar sesión con el usuario demo.
  2. Abrir R1: ver el bus moverse y el bus desactualizado (usar el control debug si hace falta).
  3. Pestaña «Cuándo pasa»: ver «~N min», mover el punto lejos y ver «Lejos de la ruta».
  4. «Buscar viaje» Valencia → Universidad del Cauca: comparar las 2 opciones.
  5. Abrir la guía de la primera opción.
  6. Abrir la app en un segundo teléfono y mostrar que los buses están en el mismo lugar.

## 7. Definición de terminado (Fase 3 y MVP)

- [ ] «Cuándo pasa» cumple CA-20.1 a CA-20.4.
- [ ] «Buscar viaje» cumple CA-21.1 a CA-21.5 y CA-22.1 a CA-22.4.
- [ ] Guía cumple CA-23.1 y CA-23.2.
- [ ] La UI no duplica lógica: los cálculos vienen de `domain/`.
- [ ] Todo el código nuevo vive en `shared/src/commonMain`; `commonMain` sigue sin importar `android.*` ni `java.*`.
- [ ] Pruebas de `domain/` siguen pasando.
- [ ] APK instalado y probado en al menos un teléfono Android real.
- [ ] README con instalación y guion de demo.

## 8. Fuera del alcance del MVP

Registro con correo y código (HU-15), acceso con Google (HU-16), GPS real del pasajero, sincronización real con buses (requiere un servicio en tiempo real en una fase posterior), app del conductor (EP-02), panel de administración (EP-01) y targets iOS, Desktop o Web (el proyecto ya es Kotlin Multiplatform, así que se podrán añadir después).

## 9. Trazabilidad

| Pantalla | HU | Reglas |
| --- | --- | --- |
| ¿Cuándo pasa? | HU-20 | RN-03, RN-08, RN-11 |
| Buscar viaje | HU-21, HU-22 | RN-03, RN-04, RN-08, RN-09, RN-10, RN-11, RN-17, DP-15 |
| Guía | HU-23 | RN-04 |
