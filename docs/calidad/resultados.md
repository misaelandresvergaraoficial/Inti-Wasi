# Resultados de calidad y pruebas

Fecha: 29/10/2026. Rama: `develop`. Spring Boot 4.1.2 · Angular 22.2.3 · MySQL 8.

## Backend

| Métrica | Resultado |
|---|---|
| Pruebas unitarias | 16 |
| Pruebas de integración | 7 |
| Total | **23 aprobadas, 0 fallos** |
| Cobertura de líneas | **41,8 %** |
| Cobertura de ramas | 27,7 % |
| Herramientas | JUnit 5, Mockito, MockMvc, JaCoCo 0.8.16 |

Comando: `cd backend && ./mvnw verify`

Reporte: `backend/target/site/jacoco/index.html` · Datos: `docs/calidad/jacoco.csv`

### Pruebas de integración

| Caso | Verificación |
|---|---|
| Login con credenciales inválidas | HTTP 401 |
| Productos sin sesión | HTTP 401 |
| Productos con Administrador | HTTP 200 y arreglo JSON |
| Movimientos paginados | HTTP 200 con `content`, `totalElements`, `totalPages` |
| Usuarios con Operador | HTTP 403 |
| OpenAPI | HTTP 200 con `info` y `paths` |
| Ruta inexistente | HTTP 404 |

## Frontend

| Métrica | Resultado |
|---|---|
| Archivos de prueba | 17 |
| Pruebas | **76 aprobadas, 0 fallos** |
| Cobertura de líneas | **69,38 %** |
| Cobertura de sentencias | 65,74 % |
| Cobertura de ramas | 62,77 % |
| Cobertura de funciones | 64,07 % |

Comando: `cd frontend && npm test -- --watch=false`

## Pruebas E2E

| Caso | Resultado |
|---|---|
| Login con contraseña incorrecta y acceso válido | Aprobado |
| Navegación entre páginas de movimientos | Aprobado |

Comando: `cd frontend && npx playwright test`

## Prueba de carga

Escenario: 5 usuarios virtuales, 25 segundos, consulta paginada del historial.

| Métrica | Resultado |
|---|---|
| Solicitudes | 79 |
| Errores HTTP | **0,00 %** |
| Checks aprobados | 100 % (157/157) |
| p95 | **35,47 ms** |
| Media | 26,53 ms |

Comando: `k6 run load/movimientos.js` con `K6_EMAIL` y `K6_PASSWORD`.

## Seguridad de dependencias

| Herramienta | Resultado |
|---|---|
| `npm audit` | 0 vulnerabilidades |
| OSV-Scanner 2.7.0 (Maven y npm) | **0 incidencias** |

Antes de la actualización se detectaron 18 vulnerabilidades transitivas (Tomcat 11.0.24 y Jackson 2.21.5/3.1.5). Se actualizó Spring Boot de 4.1.1 a **4.1.2**, que incorpora las versiones corregidas, y el escaneo quedó limpio.

## Análisis dinámico (DAST)

Herramienta: OWASP ZAP 2.17.0.

### Línea base anónima

| Hallazgo | Riesgo | Observación |
|---|---|---|
| Content Security Policy ausente | Medio | Aplica a respuestas HTML; se define en el despliegue |
| Divulgación de marcas de tiempo Unix | Bajo | Corresponden a `iat`/`exp` del JWT |
| Comentarios sospechosos | Informativo | Documentación de Swagger |
| Aplicación web moderna | Informativo | Detección de framework |

### Escaneo autenticado (pasivo)

| Métrica | Antes | Después |
|---|---|---|
| Alertas | 2 (35 instancias) | **0** |
| Rutas con HTTP 500 | 35 | 0 |

**Hallazgo corregido:** las rutas no existentes y las rutas con barra final devolvían HTTP 500 con mensaje de error interno. Se agregaron manejadores para `NoResourceFoundException` y `HttpRequestMethodNotSupportedException` en `GlobalExceptionHandler`; ahora responden 404 y 405. Se añadió una prueba de regresión.

El escaneo activo no se ejecutó para no crear ni modificar registros de negocio. Debe realizarse sobre una base desechable.

Reportes: `docs/calidad/zap-report.html` (anónimo) y `docs/calidad/zap-auth-report.html` (autenticado).

## Umbrales

| Proyecto | Umbral |
|---|---|
| Backend (JaCoCo) | 40 % de líneas en `verify` |
| Frontend (Vitest) | 60 % sentencias y líneas, 55 % ramas, 60 % funciones |

## Integración continua

`.github/workflows/calidad.yml`:

- Backend: MySQL 8 como servicio, instalación del esquema, `mvnw verify`, artefacto JaCoCo.
- Frontend: `npm ci`, pruebas con cobertura, build, artefacto de cobertura.
- Se ejecuta en `push` y `pull_request` sobre `develop` y `main`.

## Límites

- La cobertura del backend es inicial; los servicios de mayor complejidad aún no están cubiertos.
- El escaneo activo de ZAP queda pendiente sobre un entorno desechable.
- No se ejecutaron pruebas de estrés sostenido.
