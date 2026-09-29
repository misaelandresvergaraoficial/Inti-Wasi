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

| Función | API |
| --- | --- |
| Iniciar sesión | `POST /api/auth/login` |
| Verificar sesión y cuenta actual | `GET /api/auth/me` |
| Listar activos e inactivos (Administrador) | `GET /api/usuarios/todos` |
| Consultar, crear y editar | `GET /api/usuarios/{id}`, `POST /api/usuarios`, `PUT /api/usuarios/{id}` |
| Desactivar lógicamente | `DELETE /api/usuarios/{id}` (Estado = 0) |
| Reactivar | `PUT /api/usuarios/{id}/estado` con `{ "estado": 1 }` |

El listado anterior `GET /api/usuarios` sigue devolviendo únicamente activos. Editar datos personales conserva el estado existente. El acceso a usuarios y los cambios de estado están restringidos al Administrador en el servidor.

## Sesión y seguridad

Se guarda el JWT y la información mínima de la cuenta en `sessionStorage`. La expiración se toma del JWT y, al recargar, `/api/auth/me` verifica la cuenta antes de entrar a una ruta protegida. Cerrar sesión elimina los datos de la pestaña. Si un Administrador cambia su propio correo o contraseña, se le pide iniciar sesión nuevamente.

`sessionStorage` es accesible desde JavaScript: la protección contra XSS y las reglas de seguridad del servidor siguen siendo necesarias. El backend valida el JWT y el estado activo de la cuenta en las solicitudes protegidas. Los guards de Angular orientan la navegación, mientras el backend decide los permisos.

## Verificar

```powershell
npm run build
npm test -- --watch=false
```

Las pruebas de frontend comprueban las solicitudes HTTP, el JWT, la restauración de sesión y los cambios de estado. Las pruebas unitarias del backend están en `backend/src/test/java/com/intiwasi/backend`; se ejecutan con `./mvnw.cmd '-Dtest=UsuarioServiceTest,AuthControllerTest' test` desde PowerShell.

La interfaz se revisó con teclado y axe durante el prototipo. Antes de usarla en producción falta una comprobación humana con lector de pantalla, zoom nativo y cuentas reales de prueba. La aplicación no crea usuarios ficticios ni cambia datos existentes al iniciarse.
