# Frontend de acceso y usuarios · Inti Wasi

Aplicación Angular 22 integrada con la API Spring Boot de este repositorio. Implementa exclusivamente login, sesión, acceso por rol y gestión de usuarios. Usa Angular Material/CDK, Tailwind e Inter alojada localmente.

## Ejecutar

Requiere Node.js 24 y npm. Iniciar antes el backend en `http://localhost:8080` siguiendo el README de la raíz. En esta carpeta:

```powershell
npm ci
npm start
```

Abrir `http://localhost:4200`. `proxy.conf.json` dirige `/api` al backend durante el desarrollo. Para un despliegue, configurar el servidor web para servir Angular y dirigir `/api` al backend bajo el mismo origen.

Las cuentas ficticias del prototipo **no existen** aquí. Se ingresa con las credenciales de los usuarios de la base de datos del proyecto.

## Funciones y endpoints

| Función                                    | API                                                                      |
| ------------------------------------------ | ------------------------------------------------------------------------ |
| Iniciar sesión                             | `POST /api/auth/login`                                                   |
| Verificar sesión y cuenta actual           | `GET /api/auth/me`                                                       |
| Listar activos e inactivos (Administrador) | `GET /api/usuarios/todos`                                                |
| Consultar, crear y editar                  | `GET /api/usuarios/{id}`, `POST /api/usuarios`, `PUT /api/usuarios/{id}` |
| Desactivar lógicamente                     | `DELETE /api/usuarios/{id}` (Estado = 0)                                 |
| Reactivar                                  | `PUT /api/usuarios/{id}/estado` con `{ "estado": 1 }`                    |

El listado anterior `GET /api/usuarios` sigue devolviendo únicamente activos. Editar datos personales conserva el estado existente. El acceso a usuarios y los cambios de estado están restringidos al Administrador en el servidor.

### Flujo y contratos de la API

Angular envía las credenciales con `POST /api/auth/login` (`{ "correo": "...", "contrasena": "..." }`). La respuesta contiene `token`, `correo` y `rol`. Antes de abrir la sesión, el frontend consulta `GET /api/auth/me` con ese token y recibe la identidad completa. Después, un interceptor añade `Authorization: Bearer <token>` a las solicitudes protegidas de `/api`; no lo añade al login ni a URL externas. Al recargar, `/api/auth/me` vuelve a validar la sesión.

El cuerpo de `POST /api/usuarios` y `PUT /api/usuarios/{id}` contiene `nomUsuario` (máximo 50 caracteres), `correo` (correo válido, máximo 100), `rol` (`Administrador` u `Operador de Almacén`), `telefono` opcional (máximo 15) y `contrasena` (6–20 cuando se establece). En edición, omitir `contrasena` conserva la actual. La respuesta de usuario incluye `idUsuario`, esos datos públicos, `estado` (`1` activo, `0` inactivo) y `fechaRegistro`; nunca incluye la contraseña. `PUT /api/usuarios/{id}/estado` recibe `{ "estado": 1 }` para reactivar. Estas operaciones exigen rol Administrador en el backend.

El frontend muestra los mensajes del servidor sin revelar detalles internos: `400` para datos inválidos, `401` para credenciales incorrectas o sesión rechazada, `403` para acceso denegado, `409` para datos duplicados y `5xx` para fallos del servidor. Los errores de campo se muestran junto al control correspondiente. El backend sigue siendo quien valida datos, JWT, estado de cuenta y permisos.

## Sesión y seguridad

Se guarda el JWT y la información mínima de la cuenta en `sessionStorage`. La expiración se toma del JWT y, al recargar, `/api/auth/me` verifica la cuenta antes de entrar a una ruta protegida. Cerrar sesión elimina los datos de la pestaña. Si un Administrador cambia su propio correo o contraseña, se le pide iniciar sesión nuevamente.

`sessionStorage` es accesible desde JavaScript: la protección contra XSS y las reglas de seguridad del servidor siguen siendo necesarias. El backend valida el JWT y el estado activo de la cuenta en las solicitudes protegidas. Los guards de Angular orientan la navegación, mientras el backend decide los permisos.

## Verificar

```powershell
npm run build
npm test -- --watch=false
```

Las pruebas de frontend comprueban las solicitudes HTTP, la inserción restringida del JWT, la restauración de sesión, el rechazo de una sesión caducada y los cambios de estado. Las pruebas unitarias del backend están en `backend/src/test/java/com/intiwasi/backend`; se ejecutan con `./mvnw.cmd '-Dtest=UsuarioServiceTest,AuthControllerTest' test` desde PowerShell.

Para comprobar el flujo completo, iniciar backend y frontend con una base de datos de prueba y usar cuentas reales de ambos roles:

1. Entrar como Administrador; listar usuarios, crear uno, editarlo, desactivarlo y reactivarlo. Comprobar que el inactivo sigue visible y que no puede iniciar sesión mientras está desactivado.
2. Entrar como Operador; comprobar que no ve Usuarios y que tampoco puede acceder escribiendo `/usuarios` en la barra de direcciones. Verificar que el servidor rechaza una petición directa no autorizada con `403`.
3. Probar credenciales erróneas, campos inválidos y duplicados; comprobar mensajes claros y foco en el primer campo erróneo. Cerrar sesión, recargar y comprobar que las rutas protegidas requieren volver a entrar.
4. Recorrer login, tabla, formularios y diálogos con teclado; comprobar foco visible, nombres de controles y anuncios de errores y resultados. Conservar capturas y resultados de estas pruebas como evidencia de integración.

La interfaz se revisó con teclado y axe durante el prototipo. Antes de usarla en producción falta una comprobación humana con lector de pantalla, zoom nativo y cuentas reales de prueba. La aplicación no crea usuarios ficticios ni cambia datos existentes al iniciarse.
