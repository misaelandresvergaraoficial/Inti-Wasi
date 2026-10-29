# Resultados de calidad y pruebas

Fecha: 29/10/2026. Rama: `develop`. Spring Boot 4.1.2 · Angular 22.2.3 · MySQL 8.

## Backend

| Métrica | Resultado |
|---|---|
| Pruebas unitarias | 16 |
| Pruebas de integración | 6 |
| Total | **22 aprobadas, 0 fallos** |
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
- Las pruebas de integración usan datos semilla y no crean registros de negocio.
- No se ejecutó análisis dinámico (DAST) ni pruebas de estrés sostenido.
