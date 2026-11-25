# UrbanMovil · Panel web del administrador

Frontend del panel de administración (épica EP-01). React + TypeScript (Vite), Mantine, React Query, react-hook-form + zod.

Estado actual: **Fase 1** — base del proyecto, login, menú lateral, conductores y vehículos (HU-01, HU-06, HU-07, HU-08, HU-28, HU-29, HU-30). Rutas, programación y monitoreo muestran una página provisional hasta las fases 2 y 3.

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
| `npm run build` | Chequeo de tipos (`tsc -b`) y build de producción |
| `npm run lint` | oxlint |

## Estructura de carpetas

```
src/
├─ main.tsx         // punto de entrada: CSS de Mantine y <App />
├─ global.css
├─ app/             // App, router, providers, queryClient, theme
├─ api/             // client.ts (fetch + ApiError), tipos.ts y un archivo por recurso
├─ auth/            // useSesion, RequireAuth, EscuchaSesionExpirada, LoginPage
├─ layout/          // AppLayout: menú lateral (AppShell + Burger en móvil)
├─ components/      // piezas reutilizables: chips, encabezado, estado vacío, confirmación…
├─ features/
│  ├─ conductores/  // ConductoresPage, ConductorForm (drawer), DisponibilidadModal, hooks
│  └─ vehiculos/    // VehiculosPage, VehiculoForm (drawer), DesactivarModal, hooks
└─ utils/           // errores.ts, fechas.ts, motivos.ts, contrasena.ts
```

Decisiones clave:

- **Errores de la API**: `utils/errores.ts → manejarErrorFormulario` reparte `campos` bajo cada input; lo que no es de un campo va a una alerta roja del formulario (`root.servidor` de react-hook-form); `REGLA_NEGOCIO` y `NO_ENCONTRADO` van a notificación con el `mensaje` del backend; `ERROR_INTERNO` muestra un mensaje genérico, salvo cuando el backend no responde («No se pudo conectar con el servidor…»).
- **Sesión vencida**: el cliente dispara el evento `sesion-expirada` ante un `401 NO_AUTENTICADO`. `EscuchaSesionExpirada` (raíz del router) limpia la caché, avisa y lleva a `/login`. No actúa si ya se está en `/login` ni si en esta pestaña nunca hubo sesión (en ese caso `RequireAuth` redirige sin aviso).
- **Caché**: `['me']`, `['conductores', filtros]`, `['vehiculos', filtros]`. Las mutaciones invalidan por prefijo.

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
