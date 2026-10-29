# Resultados de calidad y pruebas

Fecha: 29/10/2026. Rama: `develop`.

## Backend

| Métrica | Resultado |
|---|---|
| Pruebas unitarias | 16 |
| Pruebas de integración | 6 |
| Total | **22 aprobadas, 0 fallos** |
| Cobertura de líneas | **41,8 %** |
| Cobertura de ramas | 27,7 % |
| Cobertura de instrucciones | 37,66 % |
| Herramientas | JUnit 5, Mockito, MockMvc, JaCoCo 0.8.16 |

Comando: `cd backend && ./mvnw verify`

Reporte HTML: `backend/target/site/jacoco/index.html`
Datos: `docs/calidad/jacoco.csv`

### Pruebas de integración (`IntegracionApiTest`)

| Caso | Verificación |
|---|---|
| Login con credenciales inválidas | HTTP 401 |
| Productos sin sesión | HTTP 401 |
| Productos con rol Administrador | HTTP 200 y arreglo JSON |
| Movimientos paginados | HTTP 200 con `content`, `totalElements`, `totalPages` |
| Usuarios con rol Operador | HTTP 403 |
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
| Herramientas | Vitest 4.1.12, @vitest/coverage-v8 |

Comando: `cd frontend && npm test -- --watch=false`

Reporte HTML: `frontend/coverage/inti-wasi-frontend/index.html`

## Umbrales configurados

| Proyecto | Umbral |
|---|---|
| Backend (JaCoCo) | 40 % de líneas en `verify` |
| Frontend (Vitest) | 60 % sentencias y líneas, 55 % ramas y 60 % funciones |

## Integración continua

Archivo: `.github/workflows/calidad.yml`

- Backend: MySQL 8 como servicio, esquema instalado desde `database/`, `mvnw verify`, artefacto JaCoCo.
- Frontend: `npm ci`, pruebas con cobertura, build, artefacto de cobertura.
- Se ejecuta en `push` y `pull_request` sobre `develop` y `main`.

## Límites

- La cobertura del backend es inicial; los servicios de mayor complejidad aún no están cubiertos.
- Las pruebas de integración usan datos semilla y no crean registros de negocio.
- No se ejecutaron pruebas de carga ni análisis dinámico de seguridad en esta etapa.
