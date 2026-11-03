# Frontend de Inti Wasi

Interfaz de gestión de inventario y almacén construida con Angular 22, Angular Material/CDK y Tailwind. Consume la API Spring Boot del proyecto. Incluye tema claro y oscuro, diseño adaptable, navegación por teclado, mensajes de validación y confirmación de operaciones.

## Ejecución local

1. Preparar MySQL y el backend siguiendo la [guía general](../README.md).
2. Con Node.js 24.15 o superior dentro de la versión 24 y npm, ejecutar desde `frontend/`:

   ~~~powershell
   npm ci
   npm start
   ~~~

3. Abrir `http://localhost:4200` e ingresar con una cuenta activa de la instancia.

`proxy.conf.json` dirige `/api` a `http://localhost:8080`. En producción, frontend y API deben compartir el mismo origen HTTPS.

## Módulos y funciones

**A:** Administrador. **O:** Operador de Almacén.

| Módulo y ruta | Acceso | Funciones |
|---|---|---|
| Acceso, `/login` | Sin sesión | Iniciar sesión, mostrar errores de acceso y recuperar la identidad al recargar. |
| Dashboard, `/dashboard` | A | Consultar productos activos, stock bajo, órdenes por completar, entradas y salidas del día; revisar actividad de 30 días, cinco productos prioritarios por reponer y accesos a sus módulos. Actualizar los indicadores. |
| Usuarios, `/usuarios` | A | Buscar y filtrar cuentas, consultar detalles, crear, editar, cambiar rol o contraseña, desactivar y reactivar. Impedir duplicados y conservar un administrador activo. |
| Categorías, `/categorias` | A | Buscar, filtrar por estado, consultar detalles, crear, editar, desactivar y reactivar. Confirmar el efecto sobre los productos asociados. |
| Proveedores, `/proveedores` | A y O | Buscar y consultar detalles. A administra registros, estados y prefijo de guía; O consulta activos. |
| Productos, `/productos` | A y O | Buscar, filtrar y consultar SKU, categoría, proveedor, precio y existencias. A crea, edita, desactiva, reactiva y consulta `/productos/stock-bajo`; O consulta activos. |
| Órdenes de compra, `/ordenes-compra` | A y O | Buscar por número o proveedor, filtrar por estado y fechas, ordenar, paginar y consultar detalles y saldos por recibir. A crea, edita órdenes habilitadas y cancela; O solo consulta. |
| Entradas, `/entradas` | A y O | Buscar, filtrar, ordenar, paginar, consultar detalles, registrar recepciones parciales o completas, rectificar y anular con motivo y confirmación. Consultar orden, guía, cantidades recibidas, pendientes y stock. |
| Salidas, `/salidas` | A y O | Buscar, filtrar, ordenar, paginar, consultar detalles, registrar despachos o mermas, rectificar y anular con motivo y confirmación. Comprobar disponibilidad de stock. |
| Ajustes, `/ajustes` | A | Buscar, filtrar, ordenar, paginar y consultar diferencias de inventario. Registrar incrementos o decrementos, rectificar y anular con motivo y confirmación. |
| Documentos, `/documentos` | A y O | Buscar, filtrar por tipo, estado y fechas, ordenar, paginar y consultar responsables, documento de origen y documentos relacionados. Abrir el detalle de la operación correspondiente según permisos. |
| Movimientos, `/movimientos` | A y O | Filtrar por fechas, producto, tipo de documento y responsable; ordenar, cambiar tamaño de página y consultar cada movimiento y su documento. |
| Reportes, `/reportes` | A | Consultar inventario, movimientos y reposición; combinar o limpiar filtros, paginar y descargar PDF o CSV de la consulta completa. |

Los formularios incluyen rutas de alta, detalle y edición. Se solicita confirmación al abandonar cambios sin guardar. La raíz, el logo y `/inicio` llevan al Dashboard para A y a Órdenes de compra para O; una ruta sin permiso muestra `/acceso-denegado`.

## APIs utilizadas

Todas las rutas siguientes parten del mismo origen del frontend. Las solicitudes y respuestas usan JSON, salvo las descargas.

### Acceso y sesión

| Método y endpoint | Función |
|---|---|
| `GET /api/auth/csrf` | Inicializar o renovar el token CSRF; responde `204`. |
| `POST /api/auth/login` | Validar `{ correo, contrasena }` y establecer la cookie de sesión. |
| `GET /api/auth/me` | Verificar la sesión y obtener la identidad y su vencimiento. |
| `POST /api/auth/actividad` | Registrar actividad de una sesión abierta; responde `204`. |
| `POST /api/auth/logout` | Revocar la sesión y retirar su cookie; responde `204`. |

Login y `/me` responden `{ usuario, expiresAt }`. `usuario` contiene `idUsuario`, `nomUsuario`, `correo`, `rol`, `telefono`, `estado` y `fechaRegistro`; `expiresAt` es una fecha de vencimiento expresada en milisegundos Unix. La contraseña y el JWT no forman parte de la respuesta.

### Catálogos y usuarios

En esta tabla, `{recurso}` corresponde a `usuarios`, `categorias`, `proveedores` o `productos`.

| Método y endpoint | Función |
|---|---|
| `GET /api/{recurso}` | Listar activos. |
| `GET /api/{recurso}/todos` | Listar activos e inactivos; requiere A. |
| `GET /api/{recurso}/{id}` | Consultar un registro. |
| `POST /api/{recurso}` | Crear; requiere A. |
| `PUT /api/{recurso}/{id}` | Editar; requiere A. |
| `DELETE /api/{recurso}/{id}` | Desactivar sin borrar el historial; requiere A. |
| `PUT /api/{recurso}/{id}/estado` | Reactivar con `{ estado: 1 }`; requiere A. |
| `GET /api/productos/stock-bajo` | Consultar productos activos con `stockActual <= stockMinimo`; requiere A. |

Todas las operaciones de usuarios y categorías requieren A, incluidas sus consultas. El frontend de usuarios utiliza `/todos` para administrar cuentas. O accede al listado activo y al detalle de proveedores y productos; sus registros inactivos no se ofrecen como seleccionables.

**Cuerpos de creación y edición:**

| Recurso | Campos |
|---|---|
| Usuarios | `nomUsuario`, `correo`, `rol`, `telefono` y `contrasena`. La contraseña se exige al crear; omitirla al editar conserva la actual. |
| Categorías | `nomCategoria` y `estado`: `1` activo o `0` inactivo. |
| Proveedores | `nomProveedor`, `ruc`, `prefijoGuia`, `contacto`, `telefono` y `direccion`. |
| Productos | `sku`, `nomProducto`, `idCategoria`, `idProveedor` opcional, `precio` y `stockMinimo`. |

El teléfono de usuario es opcional, único y, si se informa, tiene nueve dígitos y empieza por 9. El correo corresponde al rol: `admin.nombre@intiwasi.pe` u `operador.nombre@intiwasi.pe`. Proveedores exige RUC de once dígitos y teléfono de nueve; el prefijo de guía usa el formato `GUIA-CODIGO-`. La respuesta `prefijoGuiaBloqueado` indica cuándo ese prefijo ya no puede editarse.

El stock inicial del producto lo establece el servidor en cero. Sus existencias cambian mediante entradas, salidas y ajustes, no desde el formulario del catálogo. Desactivar una categoría afecta a sus productos; reactivarla recupera solo los que se desactivaron por esa categoría.

### Órdenes de compra

| Método y endpoint | Función |
|---|---|
| `GET /api/ordenes-compra` | Consultar y filtrar órdenes paginadas. |
| `GET /api/ordenes-compra/{id}` | Consultar cabecera, productos, precios y cantidades recibidas o pendientes. |
| `POST /api/ordenes-compra` | Crear una orden; requiere A. |
| `PUT /api/ordenes-compra/{id}` | Editar una orden habilitada; requiere A. |
| `DELETE /api/ordenes-compra/{id}` | Cancelar sin borrar el historial; requiere A. |

El cuerpo lleva `idProveedor`, `fechaEstimadaEntrega` —fecha ISO o `null`— y `detalles: [{ idProducto, cantidad, precioUnitario }]`. Las cantidades son enteros positivos y el precio admite hasta dos decimales. Los productos deben estar activos y pertenecer al proveedor elegido.

La API calcula fecha de emisión, responsable, importes y recepción acumulada. Los estados son `Pendiente`, `Parcial`, `Recibida` y `Cancelada`. Se edita solo una Pendiente sin entradas registradas y con `editable: true`. Se cancelan Pendientes o Parciales. El precio pactado se conserva aunque cambie el precio del producto.

### Entradas, salidas y ajustes

En esta tabla, `{operacion}` corresponde a `entradas`, `salidas` o `ajustes`. Entradas y Salidas admiten A y O; Ajustes requiere A.

| Método y endpoint | Función |
|---|---|
| `GET /api/{operacion}` | Consultar el historial paginado. |
| `GET /api/{operacion}/{id}` | Consultar una operación y sus movimientos. |
| `GET /api/{operacion}/documento/{idDocumento}` | Localizar la operación desde su documento. |
| `POST /api/{operacion}` | Registrar y confirmar una operación nueva. |
| `PUT /api/{operacion}/{id}` | Rectificar una operación vigente. |
| `DELETE /api/{operacion}/{id}` | Anular una operación vigente con `{ motivo }` en el cuerpo. |

Los tres cuerpos llevan `movimientos: [{ idProducto, cantidad }]` y `observaciones` opcionales. Cada cantidad representa unidades, no el stock final.

| Operación | Campos propios y reglas |
|---|---|
| Entrada | `idOrden` y `numeroGuiaRemision`. Admite órdenes Pendientes o Parciales, productos de esa orden y cantidades que no excedan el saldo. La guía completa debe respetar el prefijo del proveedor y no repetirse para otra entrega. |
| Salida | `motivo`: `Despacho/Venta`, `Merma` u `Otro`. `Otro` exige observaciones. La salida no puede superar las existencias disponibles. |
| Ajuste | `tipoAjuste`: `Incremento` o `Decremento`, y `motivo`. Las cantidades son positivas; el tipo determina si se suman o restan. Un decremento no puede dejar stock negativo. |

La rectificación añade `motivoCorreccion` y conserva la relación con el documento original. En entradas se conserva la orden y la guía; en salidas, el conjunto original de productos. El servidor valida que sea posible revertir la operación anterior y aplicar la nueva sin inconsistencias. La anulación también registra su motivo y revierte el efecto sobre inventario.

Los documentos usan `1` para Vigente, `2` para Rectificado y `0` para Anulado. Las correcciones y anulaciones permanecen consultables; no se borran registros históricos.

### Consultas y reportes

| Método y endpoint | Función |
|---|---|
| `GET /api/dashboard/resumen` | Indicadores, productos por reponer y actividad de 30 días; requiere A. |
| `GET /api/documentos` | Listar documentos paginados. |
| `GET /api/documentos/{id}` | Consultar tipo, estado, fecha, responsable y origen. |
| `GET /api/documentos/{id}/relacionados` | Consultar la cadena de documentos vinculados. |
| `GET /api/movimientos` | Listar movimientos paginados. |
| `GET /api/movimientos/{id}` | Consultar producto, cantidades, responsable y documento asociado. |
| `GET /api/movimientos/productos` | Obtener opciones del filtro de productos del historial. |
| `GET /api/movimientos/responsables` | Obtener opciones del filtro de responsables del historial. |
| `GET /api/reportes/inventario` | Existencias, categoría, proveedor y precio referencial; requiere A. |
| `GET /api/reportes/movimientos` | Historial de movimientos con cantidades con signo; requiere A. |
| `GET /api/reportes/reposicion` | Stock actual, mínimo y unidades por reponer; requiere A. |
| `GET /api/reportes/{reporte}/exportar` | Descargar `inventario`, `movimientos` o `reposicion` con `formato=pdf` o `formato=csv`; requiere A. |

Las descargas aplican los mismos filtros de la consulta, sin limitarse a la página visible. Responden con un archivo PDF o CSV UTF-8 y `Content-Disposition`; si no hay datos devuelven `204`. El máximo por exportación es de 10 000 registros.

## Filtros, paginación y respuestas

| Consulta | Parámetros |
|---|---|
| Órdenes | `q`, `estado`, `desde`, `hasta`, `sort` y `admisibles`. Este último limita el selector de recepciones a órdenes admisibles. |
| Entradas | `q`, `estado`, `desde`, `hasta` y `sort`. |
| Salidas y Ajustes | `q`, `estado`, `desde`, `hasta`, `tipo` y `sort`. |
| Documentos | `q`, `estado`, `desde`, `hasta`, `tipo` y `sort`. |
| Movimientos | `fechaInicial`, `fechaFinal`, `idProducto`, `tipoDocumento` e `idUsuario`. |
| Reportes | `idProducto` en los tres; `fechaInicial`, `fechaFinal`, `tipoMovimiento` e `idUsuario` en movimientos. |

Las fechas de filtro usan `YYYY-MM-DD`. Los listados paginados reciben `page` desde cero y `size`; responden con `content`, `totalElements`, `totalPages` y `number`. Órdenes, Entradas, Salidas y Ajustes muestran ocho registros por página; Documentos, diez; Reportes, veinte. Movimientos comienza con veinte y permite diez, veinte o cincuenta. Los catálogos se reciben como arreglos y se filtran en la interfaz.

En Órdenes y los historiales de documentos, `sort` usa `asc` o `desc`. Movimientos envía `sort=fechaEmision,desc&sort=idMovimiento,desc`, o ambos en ascendente, para conservar un orden estable.

Una creación responde normalmente `201`; una consulta o edición, `200`; una desactivación, anulación o cancelación correcta, `204`. El servidor asigna IDs, responsables, fechas, estados calculados y stock: esos valores no se envían como campos editables.

Los errores incluyen `timestamp`, `status`, `error` y `message`. La interfaz muestra errores de campo o de operación y conserva los datos del formulario cuando corresponde:

| Estado | Tratamiento |
|---|---|
| `400` | Revisar campos, cantidades, fechas o reglas de la operación. |
| `401` | Credenciales incorrectas o sesión rechazada; comprobar identidad o volver al acceso. |
| `403` | Operación sin permiso o solicitud sin CSRF válido. |
| `404` | Registro no encontrado o no disponible para el rol. |
| `409` | Duplicidad o conflicto de estado. |
| `429` | Límite temporal alcanzado; respetar `Retry-After`. |
| `5xx` | Mostrar un mensaje general, sin detalles internos del servidor. |

Los contratos TypeScript se encuentran en [core](src/app/core/) y las rutas en [app.routes.ts](src/app/app.routes.ts).

## Flujo de sesión y seguridad

1. Antes del login, Angular obtiene `XSRF-TOKEN` con `GET /api/auth/csrf`.
2. Envía las credenciales a `POST /api/auth/login` con `X-XSRF-TOKEN`. El servidor establece la cookie HttpOnly y devuelve identidad y vencimiento.
3. Angular renueva CSRF y abre el destino del rol. Las escrituras posteriores envían la cabecera CSRF; el navegador adjunta la cookie de sesión.
4. Al recargar, `GET /api/auth/me` verifica la identidad antes de permitir la navegación. El backend comprueba la sesión, el estado de cuenta y los permisos.
5. La actividad se comunica a `POST /api/auth/actividad`. La sesión dura hasta ocho horas; a los 15 minutos sin actividad aparece un aviso y se cierra 30 segundos después si no se confirma. Continuar no prolonga el vencimiento máximo.
6. Cerrar sesión llama a `POST /api/auth/logout`, revoca la sesión, retira la cookie y comunica el cierre a las otras pestañas del mismo perfil.

El JWT no se guarda en `localStorage` ni en `sessionStorage` y no se envía mediante Bearer desde Angular. La identidad se mantiene en memoria; las preferencias de tema y señales de actividad no contienen el JWT. La cookie CSRF sí es legible por Angular para construir su cabecera.

Los guards controlan la navegación de la interfaz; los permisos se validan también en el servidor. Cambiar el propio correo o contraseña requiere volver a iniciar sesión. En HTTP local se usa `app.auth.cookie-secure=false`; con HTTPS, incluido producción, `true`. La cookie de sesión utiliza `SameSite=Strict`.

## Verificación

Desde `frontend/`:

~~~powershell
npm run build
npm test -- --watch=false
~~~

La compilación genera `dist/inti-wasi-frontend/browser/`. Las pruebas unitarias revisan servicios HTTP, formularios, validaciones y comportamiento de sesión. Para revisar el contrato completo de cada servicio, consultar Swagger con una sesión Administrador en `http://localhost:8080/swagger-ui/index.html`.
