---
title: "IntegraciÃ³n Avanzada, Seguridad y DocumentaciÃ³n de la API"
subtitle: "Sistema de GestiÃ³n de Inventario y AlmacÃ©n â€” Inti Wasi"
author: "Grupo 03"
date: "29 de octubre de 2026"
lang: es-ES
---

::: {custom-style="Portada"}
**Curso:** Soluciones Web y Aplicaciones Distribuidas

**Docente:** Jhon Paul Anampa Garcia

**Integrantes:** Misael AndrÃ©s Vergara Morales; Kevin AndrÃ© de La Cruz Villugas; Alvaro Luis Suica Salcedo; Israel Rodrigo Ochoa MejÃ­a; Yul Alexander Retamozo Gutierrez; Jorge Mateo Aylas Ynciso

**Periodo acadÃ©mico:** 2026

**TÃ­tulo del proyecto:** AplicaciÃ³n Web de GestiÃ³n de Inventario y AlmacÃ©n â€” Inti Wasi

**EvaluaciÃ³n:** T3
:::

```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

# IntroducciÃ³n

La EvaluaciÃ³n T3 consolida la integraciÃ³n entre el frontend Angular y el backend Spring Boot del sistema de inventario de Distribuidora Inti Wasi S.A.C. Desde la T2 se incorporaron la autenticaciÃ³n con JWT, la protecciÃ³n de rutas, el consumo de endpoints protegidos, la paginaciÃ³n de resultados y la documentaciÃ³n de la API con OpenAPI.

El sistema cubre los procesos de usuarios, categorÃ­as, proveedores, productos, Ã³rdenes de compra, entradas, salidas, ajustes, documentos, movimientos, dashboard y reportes. La soluciÃ³n se encuentra en estado funcional y verificable en entorno local, con evidencia de ejecuciÃ³n de pruebas y capturas de cada mecanismo incorporado.

# Arquitectura de integraciÃ³n

La soluciÃ³n mantiene una arquitectura cliente-servidor desacoplada en tres capas: cliente Angular 22, API REST con Spring Boot 4.1.2 sobre Java 25, y persistencia MySQL 8. La comunicaciÃ³n utiliza HTTP con mensajes JSON y autenticaciÃ³n mediante token Bearer.

![**Figura 1.** *Arquitectura de integraciÃ³n cliente-servidor del sistema Inti Wasi.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/diagramas/diagrama de paquetes/Diagrama_paquetes_IntiWasi.png>)

El cliente organiza su cÃ³digo en `core` (servicios, guards, interceptor y modelos), `features` (mÃ³dulos funcionales), `layout` (estructura visual) y `shared` (componentes reutilizables). El backend separa seguridad, controladores, servicios, repositorios y entidades, con 13 controladores REST.

# DiseÃ±o de interfaces

La interfaz utiliza Angular Material y un sistema visual propio con temas claro y oscuro. La navegaciÃ³n se organiza por rol: el Administrador accede a dashboard, reportes y ajustes; el Operador accede a documentos, movimientos y operaciones de almacÃ©n.

![**Figura 2.** *Pantalla de inicio de sesiÃ³n.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/01-login.png>)

![**Figura 3.** *Panel principal con sesiÃ³n autenticada.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/04-inicio-autenticado.png>)

El flujo de navegaciÃ³n parte del login, continÃºa al panel principal y desde allÃ­ a los mÃ³dulos habilitados segÃºn el rol. Las rutas no autorizadas redirigen al login o a la pantalla de acceso denegado.

# RelaciÃ³n con el ODS 9

El proyecto contribuye al Objetivo de Desarrollo Sostenible 9 en dos metas. La meta 9.4 se aborda al modernizar la infraestructura tecnolÃ³gica de la distribuidora con una plataforma web que sustituye registros manuales y hojas de cÃ¡lculo. La meta 9.5 se cumple al incorporar innovaciÃ³n tecnolÃ³gica en una pequeÃ±a empresa comercial mediante control de inventario, trazabilidad y reportes.

Los beneficios esperados son la reducciÃ³n de discrepancias de stock, la conservaciÃ³n del historial de movimientos, la disminuciÃ³n de tiempos de atenciÃ³n y la disponibilidad de informaciÃ³n para decisiones de abastecimiento.

# Seguridad implementada

La autenticaciÃ³n se basa en JSON Web Tokens firmados con HMAC. El flujo es el siguiente:

1. El usuario envÃ­a correo y contraseÃ±a a `POST /api/auth/login`.
2. El backend valida las credenciales con BCrypt y verifica que la cuenta estÃ© activa.
3. Se genera un token con expiraciÃ³n configurable y se devuelve al cliente.
4. El cliente consulta `GET /api/auth/me` con el token para obtener identidad y rol.
5. La sesiÃ³n se almacena en `sessionStorage` con la clave `intiwasi.session`.
6. Un interceptor HTTP agrega `Authorization: Bearer <token>` a cada solicitud protegida.

La protecciÃ³n de rutas se implementa con `authGuard`, que bloquea el acceso sin sesiÃ³n vÃ¡lida, y `guestGuard`, que impide mostrar el login a un usuario autenticado. El cierre de sesiÃ³n elimina el token y redirige al login. La inactividad de quince minutos muestra un aviso de treinta segundos antes de cerrar la sesiÃ³n.

![**Figura 4.** *RedirecciÃ³n automÃ¡tica al login al solicitar una ruta protegida sin sesiÃ³n.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/02-guard-redireccion.png>)

# IntegraciÃ³n frontend-backend

Los servicios Angular consumen los endpoints del backend mediante `HttpClient`. Cada mÃ³dulo cuenta con un servicio que centraliza las operaciones y transforma los errores HTTP en mensajes para el usuario.

| MÃ³dulo | Endpoint principal | Operaciones |
|---|---|---|
| AutenticaciÃ³n | `/api/auth` | Login e identidad |
| Usuarios | `/api/usuarios` | Listar, crear, editar, desactivar |
| Productos | `/api/productos` | Listar, crear, editar, eliminar, stock bajo |
| Proveedores | `/api/proveedores` | Listar, crear, editar, desactivar |
| Ã“rdenes de compra | `/api/ordenes-compra` | Listar, crear, editar, anular |
| Entradas y salidas | `/api/entradas`, `/api/salidas` | Registrar, rectificar, anular |
| Movimientos | `/api/movimientos` | Consultar paginado y detalle |

: **Tabla 1.** *Endpoints principales consumidos por el frontend*

Los errores 401 cierran la sesiÃ³n y redirigen al login; los errores 403 muestran acceso denegado; los errores 400 presentan el mensaje de validaciÃ³n del backend.

# PaginaciÃ³n

El backend expone `GET /api/movimientos` con parÃ¡metros `page`, `size` y `sort`, y devuelve un objeto `Page` con `content`, `totalElements`, `totalPages` y `number`. El frontend consume la pÃ¡gina solicitada y presenta los resultados con `MatPaginator`, con tamaÃ±os de pÃ¡gina de 10, 20 y 50 registros.

![**Figura 5.** *Listado de movimientos con paginaciÃ³n del lado del servidor.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/05-paginacion-movimientos.png>)

La paginaciÃ³n reduce el volumen de datos transferidos y mantiene los filtros de fecha, producto, tipo de documento y usuario.

# DocumentaciÃ³n de la API

La documentaciÃ³n se genera con springdoc-openapi 3.2.0. La interfaz Swagger UI estÃ¡ disponible en `/swagger-ui/index.html` y el contrato JSON en `/v3/api-docs`. El esquema incluye autenticaciÃ³n Bearer para probar endpoints protegidos desde la interfaz.

![**Figura 6.** *Swagger UI con los servicios documentados y el esquema de autorizaciÃ³n JWT.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/03-swagger.png>)

La documentaciÃ³n expone 38 rutas agrupadas por controlador y permite ejecutar solicitudes con el token obtenido en el login.

# Evidencias de pruebas

| Conjunto | Herramienta | Resultado |
|---|---|---|
| Pruebas unitarias backend | JUnit y Mockito | 16 aprobadas, 0 fallos |
| Pruebas de integración backend | MockMvc | 6 aprobadas, 0 fallos |
| Pruebas unitarias frontend | Vitest | 76 aprobadas en 17 archivos |
| CompilaciÃ³n frontend | Angular CLI | Correcta |
| VerificaciÃ³n funcional | Playwright y capturas | Login, guard, paginaciÃ³n, CRUD, Swagger y logout |

: **Tabla 2.** *Resultados de las pruebas ejecutadas*

![**Figura 7.** *Operaciones CRUD del mÃ³dulo de productos.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/06-crud-productos.png>)

![**Figura 8.** *Cierre de sesiÃ³n y retorno al login.*](<C:/agentes/agente-solucionesweb-calidad/proyecto/Inti-Wasi/docs/evidencias/T3/07-logout.png>)

Las capturas se generaron sobre la aplicaciÃ³n en ejecuciÃ³n con backend, frontend y base de datos reales.

# Conclusiones

1. La autenticaciÃ³n con JWT y la polÃ­tica stateless permiten proteger los servicios sin mantener estado en el servidor.
2. Los guards de Angular y el interceptor HTTP complementan la seguridad del backend y evitan el acceso a rutas sin sesiÃ³n vÃ¡lida.
3. La paginaciÃ³n del lado del servidor con `MatPaginator` mejora el rendimiento y la experiencia de consulta en el historial de movimientos.
4. La incorporaciÃ³n de Swagger/OpenAPI facilita la verificaciÃ³n de contratos y reduce el tiempo de integraciÃ³n con el frontend.
5. La ejecuciÃ³n conjunta de 22 pruebas backend y 76 frontend, junto con las capturas funcionales, evidencia una integraciÃ³n estable del sistema.

# Referencias

Angular. (2026). *Angular documentation*. https://angular.dev

OpenAPI Initiative. (2021). *OpenAPI Specification v3.1.0*. https://spec.openapis.org/oas/v3.1.0

Spring. (2026). *Spring Boot reference documentation*. https://docs.spring.io/spring-boot/

Springdoc. (2026). *springdoc-openapi documentation*. https://springdoc.org

OWASP Foundation. (2021). *OWASP Top 10:2021*. https://owasp.org/Top10/
