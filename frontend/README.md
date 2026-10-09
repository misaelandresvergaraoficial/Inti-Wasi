# Frontend de Inti Wasi

Aplicación Angular 22 integrada con la API Spring Boot de este repositorio. Implementa login, sesión, acceso por rol, usuarios, categorías, proveedores, productos, órdenes de compra, dashboard y reportes. Usa Angular Material/CDK, Tailwind e Inter alojada localmente.

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

Proveedores permite consultar los activos con ambos roles mediante `GET /api/proveedores`. El Administrador puede consultar el catálogo completo mediante `GET /api/proveedores/todos`, crear (`POST /api/proveedores`), editar (`PUT /api/proveedores/{id}`), desactivar (`DELETE /api/proveedores/{id}`) y reactivar (`PUT /api/proveedores/{id}/estado` con `{ "estado": 1 }`). La pantalla del Administrador incluye activos e inactivos y un filtro por estado; el Operador solo consulta activos. El formulario valida nombre (máximo 100 caracteres), RUC (11 dígitos), teléfono obligatorio (máximo 20), contacto opcional (máximo 100) y dirección opcional (máximo 150).

Órdenes de compra usa `GET /api/ordenes-compra` y `GET /api/ordenes-compra/{id}` para consultar; `POST /api/ordenes-compra` para crear; `PUT /api/ordenes-compra/{id}` para editar; y `DELETE /api/ordenes-compra/{id}` para **cancelar sin borrar**. El formulario consulta los proveedores y productos activos mediante `GET /api/proveedores` y `GET /api/productos`; no administra esos catálogos. El Operador de Almacén solo consulta órdenes. El Administrador puede crearlas y cancelar las Pendientes o Parciales. Solo se edita una Pendiente sin entradas registradas.

Productos usa `GET /api/productos` para el catálogo activo que consulta el Operador y `GET /api/productos/todos` para el listado completo del Administrador. En `GET /api/productos/{id}`, un producto inactivo solo es visible para el Administrador; al Operador se le responde como no encontrado. Solo el Administrador puede crear (`POST /api/productos`), editar (`PUT /api/productos/{id}`), desactivar lógicamente (`DELETE /api/productos/{id}`) y reactivar (`PUT /api/productos/{id}/estado` con `{ "estado": 1 }`). La pantalla de stock bajo consulta `GET /api/productos/stock-bajo`. El formulario usa las categorías y los proveedores activos; al crear, el backend fija el stock actual en cero. El listado muestra el proveedor asociado y el precio de compra referencial en soles. El proveedor es opcional en el catálogo, pero obligatorio para incluir el producto en una orden. El stock actual se modifica mediante los movimientos de inventario, no desde el formulario del producto.

El cuerpo de creación y edición lleva `idProveedor`, `fechaEstimadaEntrega` (fecha ISO o `null`) y `detalles` con `idProducto`, `cantidad` entera positiva y `precioUnitario` con hasta dos decimales. Solo se admiten productos activos asignados al proveedor elegido; el backend también comprueba esta coincidencia. Al elegir un producto, su precio de compra referencial se propone como precio unitario editable. Cada orden conserva el precio pactado aunque luego cambie el catálogo. El backend establece la fecha de emisión, el usuario responsable, el estado inicial `Pendiente`, los importes y las cantidades recibidas. Las órdenes pueden pasar a `Parcial`, `Recibida` o `Cancelada` sin perder su historial. La respuesta actual no indica si una orden Pendiente tuvo entradas posteriormente anuladas; en ese caso la API puede rechazar una edición que parecía disponible en pantalla, y el formulario muestra la razón sin descartar los datos ingresados.

El listado anterior `GET /api/usuarios` sigue devolviendo únicamente activos. Editar datos personales conserva el estado existente. El acceso a usuarios y los cambios de estado están restringidos al Administrador en el servidor.

El Dashboard del Administrador consulta `GET /api/dashboard/resumen` al abrir o reintentar. Presenta productos activos, productos con stock bajo, entradas y salidas del día y hasta 20 productos por reponer. Desde allí se abre el reporte de reposición completo. No exporta archivos.

Reportes consulta `GET /api/reportes/inventario`, `/movimientos` y `/reposicion` con páginas de 20 registros. El filtro por producto se aplica a los tres; fechas, tipo de movimiento y usuario se aplican al historial. Cuando hay resultados, `GET /api/reportes/{reporte}/exportar?formato=pdf|excel` descarga la consulta completa en PDF o Excel XML (`.xls`). La API devuelve 204 si no hay datos y rechaza las exportaciones de más de 10 000 registros. Dashboard y reportes están protegidos en Angular y Spring Security para el Administrador.

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

Las pruebas de frontend comprueban las solicitudes HTTP, la inserción restringida del JWT, la restauración de sesión, el rechazo de una sesión caducada y los cambios de estado. Las pruebas unitarias del backend están en `backend/src/test/java/com/intiwasi/backend`; se ejecutan con `./mvnw.cmd test` desde PowerShell.

### Comparar Selenium y Playwright en el inicio de sesión

Con MySQL, backend y frontend en ejecución, usar una cuenta Administrador activa de prueba. En una terminal situada en `frontend/`:

```powershell
$env:E2E_ADMIN_EMAIL = 'correo-de-prueba'
$env:E2E_ADMIN_PASSWORD = 'contraseña-de-prueba'
npm run test:e2e             # Selenium WebDriver
npm run test:e2e:playwright  # Playwright Test
```

Ambos casos abren Microsoft Edge, inician sesión y verifican que aparece la página Usuarios. Selenium guarda capturas en `screenshots/semana08/` y Playwright en `screenshots/semana08/playwright/`. Playwright también genera un reporte HTML local que se puede abrir con `npx playwright show-report`. La contraseña se lee de la terminal; no se guarda en los scripts ni en las capturas.

Para comprobar el flujo completo, iniciar backend y frontend con una base de datos de prueba y usar cuentas reales de ambos roles:

1. Entrar como Administrador; listar usuarios, crear uno, editarlo, desactivarlo y reactivarlo. Comprobar que el inactivo sigue visible y que no puede iniciar sesión mientras está desactivado.
2. Entrar como Operador; comprobar que no ve Usuarios y que tampoco puede acceder escribiendo `/usuarios` en la barra de direcciones. Verificar que el servidor rechaza una petición directa no autorizada con `403`.
3. Probar credenciales erróneas, campos inválidos y duplicados; comprobar mensajes claros y foco en el primer campo erróneo. Cerrar sesión, recargar y comprobar que las rutas protegidas requieren volver a entrar.
4. Recorrer login, tabla, formularios y diálogos con teclado; comprobar foco visible, nombres de controles y anuncios de errores y resultados. Conservar capturas y resultados de estas pruebas como evidencia de integración.

Para órdenes de compra, consultar listado y detalle con ambos roles y comprobar que el Operador no dispone de crear, editar ni cancelar. Como Administrador, crear una orden con varios productos; probar proveedor ausente, producto de otro proveedor o sin proveedor, producto duplicado, cantidad no entera, precio negativo y fecha anterior a la emisión. Cambiar el proveedor durante la edición debe conservar los renglones e indicar cuáles deben corregirse. Comprobar que el precio se propone al seleccionar un producto, puede editarse y no cambia en órdenes anteriores al actualizar el catálogo. Editar una Pendiente sin entradas; intentar editar una con recepción; cancelar una Pendiente y una Parcial; y comprobar que las órdenes Canceladas siguen en el listado con las cantidades recibidas intactas. Verificar navegación por teclado y presentación a 1024 × 768 y con zoom en Chrome, Firefox y Edge.

Para Productos, comprobar que el Operador solo recibe activos y no puede entrar por URL a crear, editar o ver stock bajo. Con Administrador, probar filtros y búsqueda, crear con SKU único, editar, desactivar y reactivar; el inactivo debe seguir en el listado. Probar SKU duplicado, nombres en blanco, precio con más de dos decimales, stock mínimo fraccionario, error de carga y Cancelar con cambios sin guardar. Comprobar teclado, foco, zoom y anuncios de error con lector de pantalla.

La interfaz se revisó con teclado y axe durante el prototipo. Antes de usarla en producción falta una comprobación humana con lector de pantalla, zoom nativo y cuentas reales de prueba. La aplicación no crea usuarios ficticios ni cambia datos existentes al iniciarse.
