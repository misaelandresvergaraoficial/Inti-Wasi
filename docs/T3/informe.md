---
title: "Integración Avanzada, Seguridad y Documentación de la API"
subtitle: "Sistema de Gestión de Inventario y Almacén — Inti Wasi"
author: "Grupo 03"
date: "29 de octubre de 2026"
lang: es-ES
---

::: {custom-style="Portada"}
**Curso:** Soluciones Web y Aplicaciones Distribuidas

**Docente:** Jhon Paul Anampa Garcia

**Integrantes:** Misael Andrés Vergara Morales; Kevin André de La Cruz Villugas; Alvaro Luis Suica Salcedo; Israel Rodrigo Ochoa Mejía; Yul Alexander Retamozo Gutierrez; Jorge Mateo Aylas Ynciso

**Periodo académico:** 2026

**Título del proyecto:** Aplicación Web de Gestión de Inventario y Almacén — Inti Wasi

**Evaluación:** T3
:::

```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

# Introducción

La Evaluación T3 consolida la integración entre el frontend Angular y el backend Spring Boot del sistema de inventario de Distribuidora Inti Wasi S.A.C. Desde la T2 se incorporaron la autenticación con JWT, la protección de rutas, el consumo de endpoints protegidos, la paginación de resultados y la documentación de la API con OpenAPI.

El sistema cubre los procesos de usuarios, categorías, proveedores, productos, órdenes de compra, entradas, salidas, ajustes, documentos, movimientos, dashboard y reportes. La solución se encuentra en estado funcional y verificable en entorno local, con evidencia de ejecución de pruebas y capturas de cada mecanismo incorporado.

# Arquitectura de integración

La solución mantiene una arquitectura cliente-servidor desacoplada en tres capas: cliente Angular 22, API REST con Spring Boot 4.1.2 sobre Java 25, y persistencia MySQL 8. La comunicación utiliza HTTP con mensajes JSON y autenticación mediante token Bearer.

![**Figura 1.** *Arquitectura de integración cliente-servidor del sistema Inti Wasi.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/diagramas/diagrama de paquetes/Diagrama_paquetes_IntiWasi.png>)

El cliente organiza su código en `core` (servicios, guards, interceptor y modelos), `features` (módulos funcionales), `layout` (estructura visual) y `shared` (componentes reutilizables). El backend separa seguridad, controladores, servicios, repositorios y entidades, con 13 controladores REST.

# Diseño de interfaces

La interfaz utiliza Angular Material y un sistema visual propio con temas claro y oscuro. La navegación se organiza por rol: el Administrador accede a dashboard, reportes y ajustes; el Operador accede a documentos, movimientos y operaciones de almacén.

![**Figura 2.** *Pantalla de inicio de sesión.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/01-login.png>)

![**Figura 3.** *Panel principal con sesión autenticada.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/04-inicio-autenticado.png>)

El flujo de navegación parte del login, continúa al panel principal y desde allí a los módulos habilitados según el rol. Las rutas no autorizadas redirigen al login o a la pantalla de acceso denegado.

# Relación con el ODS 9

El proyecto contribuye al Objetivo de Desarrollo Sostenible 9 en dos metas. La meta 9.4 se aborda al modernizar la infraestructura tecnológica de la distribuidora con una plataforma web que sustituye registros manuales y hojas de cálculo. La meta 9.5 se cumple al incorporar innovación tecnológica en una pequeña empresa comercial mediante control de inventario, trazabilidad y reportes.

Los beneficios esperados son la reducción de discrepancias de stock, la conservación del historial de movimientos, la disminución de tiempos de atención y la disponibilidad de información para decisiones de abastecimiento.

# Seguridad implementada

La autenticación se basa en JSON Web Tokens firmados con HMAC. El flujo es el siguiente:

1. El usuario envía correo y contraseña a `POST /api/auth/login`.
2. El backend valida las credenciales con BCrypt y verifica que la cuenta esté activa.
3. Se genera un token con expiración configurable y se devuelve al cliente.
4. El cliente consulta `GET /api/auth/me` con el token para obtener identidad y rol.
5. La sesión se almacena en `sessionStorage` con la clave `intiwasi.session`.
6. Un interceptor HTTP agrega `Authorization: Bearer <token>` a cada solicitud protegida.

La protección de rutas se implementa con `authGuard`, que bloquea el acceso sin sesión válida, y `guestGuard`, que impide mostrar el login a un usuario autenticado. El cierre de sesión elimina el token y redirige al login. La inactividad de quince minutos muestra un aviso de treinta segundos antes de cerrar la sesión.

![**Figura 4.** *Redirección automática al login al solicitar una ruta protegida sin sesión.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/02-guard-redireccion.png>)

# Integración frontend-backend

Los servicios Angular consumen los endpoints del backend mediante `HttpClient`. Cada módulo cuenta con un servicio que centraliza las operaciones y transforma los errores HTTP en mensajes para el usuario.

| Módulo | Endpoint principal | Operaciones |
|---|---|---|
| Autenticación | `/api/auth` | Login e identidad |
| Usuarios | `/api/usuarios` | Listar, crear, editar, desactivar |
| Productos | `/api/productos` | Listar, crear, editar, eliminar, stock bajo |
| Proveedores | `/api/proveedores` | Listar, crear, editar, desactivar |
| Órdenes de compra | `/api/ordenes-compra` | Listar, crear, editar, anular |
| Entradas y salidas | `/api/entradas`, `/api/salidas` | Registrar, rectificar, anular |
| Movimientos | `/api/movimientos` | Consultar paginado y detalle |

: **Tabla 1.** *Endpoints principales consumidos por el frontend*

Los errores 401 cierran la sesión y redirigen al login; los errores 403 muestran acceso denegado; los errores 400 presentan el mensaje de validación del backend.

# Paginación

El backend expone `GET /api/movimientos` con parámetros `page`, `size` y `sort`, y devuelve un objeto `Page` con `content`, `totalElements`, `totalPages` y `number`. El frontend consume la página solicitada y presenta los resultados con `MatPaginator`, con tamaños de página de 10, 20 y 50 registros.

![**Figura 5.** *Listado de movimientos con paginación del lado del servidor.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/05-paginacion-movimientos.png>)

La paginación reduce el volumen de datos transferidos y mantiene los filtros de fecha, producto, tipo de documento y usuario.

# Documentación de la API

La documentación se genera con springdoc-openapi 3.2.0. La interfaz Swagger UI está disponible en `/swagger-ui/index.html` y el contrato JSON en `/v3/api-docs`. El esquema incluye autenticación Bearer para probar endpoints protegidos desde la interfaz.

![**Figura 6.** *Swagger UI con los servicios documentados y el esquema de autorización JWT.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/03-swagger.png>)

La documentación expone 38 rutas agrupadas por controlador y permite ejecutar solicitudes con el token obtenido en el login.

# Evidencias de pruebas

| Conjunto | Herramienta | Resultado |
|---|---|---|
| Pruebas unitarias backend | JUnit y Mockito | 16 aprobadas, 0 fallos |
| Pruebas de integración backend | MockMvc | 7 aprobadas, 0 fallos |
| Pruebas unitarias frontend | Vitest | 76 aprobadas en 17 archivos |
| Compilación frontend | Angular CLI | Correcta |
| Verificación funcional | Playwright y capturas | Login, guard, paginación, CRUD, Swagger y logout |

: **Tabla 2.** *Resultados de las pruebas ejecutadas*

![**Figura 7.** *Operaciones CRUD del módulo de productos.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/06-crud-productos.png>)

![**Figura 8.** *Cierre de sesión y retorno al login.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/07-logout.png>)

Las capturas se generaron sobre la aplicación en ejecución con backend, frontend y base de datos reales.

# Conclusiones

1. La autenticación con JWT y la política stateless permiten proteger los servicios sin mantener estado en el servidor.
2. Los guards de Angular y el interceptor HTTP complementan la seguridad del backend y evitan el acceso a rutas sin sesión válida.
3. La paginación del lado del servidor con `MatPaginator` mejora el rendimiento y la experiencia de consulta en el historial de movimientos.
4. La incorporación de Swagger/OpenAPI facilita la verificación de contratos y reduce el tiempo de integración con el frontend.
5. La ejecución conjunta de 23 pruebas backend y 76 frontend, junto con las capturas funcionales, evidencia una integración estable del sistema.

# Referencias

Angular. (2026). *Angular documentation*. https://angular.dev

OpenAPI Initiative. (2021). *OpenAPI Specification v3.1.0*. https://spec.openapis.org/oas/v3.1.0

Spring. (2026). *Spring Boot reference documentation*. https://docs.spring.io/spring-boot/

Springdoc. (2026). *springdoc-openapi documentation*. https://springdoc.org

OWASP Foundation. (2021). *OWASP Top 10:2021*. https://owasp.org/Top10/
