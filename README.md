# UrbanMóvil

Sistema de gestión de rutas y buses urbanos.

| Carpeta | Contenido |
| --- | --- |
| [backend/](backend/) | API REST en Kotlin + Spring Boot + PostgreSQL. Ver [backend/README.md](backend/README.md) |
| [frontend/](frontend/) | Panel web del administrador (pendiente) |
| [FRONTEND-API.md](FRONTEND-API.md) | Contrato de la API para el frontend: endpoints, tipos y errores |

## Arranque rápido del backend

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=demo'
```

La API queda en `http://localhost:8080/api/v1` y Swagger en `http://localhost:8080/swagger-ui.html`.
