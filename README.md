# Martillo — Web API de subastas de vehículos (Backend)

## 🔗 Sitio publicado: **https://axelherrera11.github.io/subasta-vehiculos-frontend/**

API: https://subasta-vehiculos-backend.onrender.com (salud: `/api/salud`) · Frontend: https://github.com/AxelHerrera11/subasta-vehiculos-frontend

## 👥 Usuarios de prueba

| Usuario | Correo | Contraseña |
| --- | --- | --- |
| Ana López | `ana@subastas.gt` | `Subasta#2026a` |
| Bruno Méndez | `bruno@subastas.gt` | `Subasta#2026b` |
| Carla Ruiz | `carla@subastas.gt` | `Subasta#2026c` |

Se crean automáticamente (con contraseña BCrypt) al arrancar la API si no existen (`DataSeeder`).

## Arquitectura

- **Spring Boot 3 / Java 21**, Spring Security con **JWT**, `JdbcTemplate`.
- **SQL Server** (`db_WebDevUMG`); todos los objetos llevan el sufijo `_3193`. Script en [`database/subasta_3193.sql`](database/subasta_3193.sql).
- **Tiempo real** con WebSocket + STOMP (`/ws`):
  - `/topic/subastas` y `/topic/subasta/{id}`: nueva oferta o cierre. **Nunca** incluyen la identidad del postor.
  - `/user/queue/estado`: aviso privado *GANANDO* / *SUPERADO* / *GANASTE*.
- **Reglas de puja en servidor** (`sp_RegistrarPuja_3193`, con bloqueo `UPDLOCK` para ofertas simultáneas): la subasta debe estar abierta y dentro de su horario; la primera oferta ≥ monto base; las siguientes ≥ oferta actual + 10%; el dueño no puede ofertar por su vehículo.
- **Cierre automático**: un `@Scheduled` cada 5 s marca las subastas vencidas como `VENDIDA` o `DESIERTA` (sin ofertas que alcanzaran la base) y lo notifica en vivo.

## Endpoints

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/api/auth/registro`, `/api/auth/login` | Público |
| GET | `/api/auth/yo` | Sesión |
| GET | `/api/catalogos` | Público |
| GET | `/api/vehiculos?marcaId=&modeloId=&anioDesde=&nivelDanioId=&estado=&orden=…` | Público |
| GET | `/api/vehiculos/{id}`, `/api/vehiculos/{id}/pujas` | Público |
| POST | `/api/vehiculos` | Sesión |
| PUT | `/api/vehiculos/{id}` | Sesión (dueño, sin ofertas) |
| GET | `/api/mis-publicaciones?q=` | Sesión |
| POST | `/api/vehiculos/{id}/pujas` | Sesión |
| GET | `/api/vehiculos/{id}/mi-estado` | Sesión |

## Variables de entorno (Render)

Ver `.env.example`. **Las credenciales de la base de datos no se suben al repositorio**: se configuran como variables de entorno en Render.

---
Axel Herrera · Carné 1890-23-3193 · Desarrollo y Diseño Web · UMG Guastatoya
