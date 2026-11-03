# Inti Wasi · Gestión de inventario y almacén

Aplicación web para Distribuidora Inti Wasi S.A.C., desarrollada con Angular y Spring Boot. Incluye usuarios, categorías, proveedores, productos, compras, entradas, salidas, ajustes, documentos, movimientos y reportes.

El Administrador accede al Dashboard y administra el sistema. El Operador inicia en Órdenes de compra y trabaja según los permisos de su rol. Los documentos y movimientos conservan la trazabilidad del inventario.

## Requisitos

- Java 25.
- MySQL 8.0 o superior.
- Node.js 24.15 o superior dentro de la versión 24, y npm.
- PowerShell 7 para los comandos de esta guía en Windows.

Maven se ejecuta con el wrapper incluido en `backend/`.

## 1. Preparar la base de datos

**El primer script elimina y reconstruye `bd_intiwasi`. Usar una base de pruebas y respaldar cualquier información que deba conservarse.**

1. Abrir MySQL Workbench y conectarse al servidor local.
2. Ejecutar los archivos de `database/` en este orden:
   1. `01_DDL_bd_intiwasi.sql`
   2. `02_Triggers_bd_intiwasi.sql`
   3. `03_DML_bd_intiwasi.sql`
   4. `04_Vistas_bd_intiwasi.sql`
3. Crear una cuenta MySQL para el entorno local, reemplazando el marcador de contraseña:

   ~~~sql
   CREATE USER 'inti'@'localhost' IDENTIFIED BY 'TU_PASSWORD_LOCAL';
   GRANT ALL PRIVILEGES ON bd_intiwasi.* TO 'inti'@'localhost';
   ~~~

Estos pasos corresponden a una instalación nueva. Una base existente debe actualizarse mediante una migración que conserve sus datos.

## 2. Configurar el backend

1. Abrir una terminal en la raíz del repositorio:

   ~~~powershell
   cd backend
   if (-not (Test-Path src/main/resources/application-local.properties)) {
       Copy-Item src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   }
   ~~~

   Si el archivo local ya existe, conservarlo y revisar sus valores.

2. Editar `src/main/resources/application-local.properties` con la URL, el usuario y la contraseña de MySQL. Para ejecutar la aplicación en HTTP local, añadir:

   ~~~properties
   app.auth.cookie-secure=false
   ~~~

3. En una instalación nueva, generar la clave JWT y sustituir el marcador de la plantilla sin mostrar la clave en pantalla:

   ~~~powershell
   $archivo = "src/main/resources/application-local.properties"
   $clave = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
   $texto = [IO.File]::ReadAllText($archivo).Replace("TU_CLAVE_SECRETA_JWT_LOCAL", $clave)
   [IO.File]::WriteAllText($archivo, $texto)
   Remove-Variable clave, texto
   ~~~

   El archivo local contiene credenciales y debe permanecer fuera de Git. En producción se utiliza HTTPS con `app.auth.cookie-secure=true`.

4. Iniciar el backend desde `backend/`:

   ~~~powershell
   .\mvnw.cmd spring-boot:run
   ~~~

   La API queda disponible en `http://localhost:8080`.

## 3. Iniciar el frontend

En otra terminal, desde la raíz del repositorio:

~~~powershell
cd frontend
npm ci
npm start
~~~

Abrir `http://localhost:4200` e iniciar sesión con una cuenta activa de la base de datos. Las credenciales iniciales deben obtenerse del responsable de la instancia; no se publican en el repositorio. El proxy de desarrollo dirige `/api` al backend local.

La sesión usa una cookie HttpOnly y protección CSRF. Dura hasta ocho horas; después de 15 minutos de inactividad aparece un aviso y, si no se confirma, se cierra 30 segundos después.

## Documentación

- [Guía del frontend](frontend/README.md).
- [Plantilla de configuración de producción](deploy/application-production.properties.example).
- [Documentos del proyecto](docs/documentos/).
- Swagger: `http://localhost:8080/swagger-ui/index.html`, disponible después de iniciar sesión como Administrador.
