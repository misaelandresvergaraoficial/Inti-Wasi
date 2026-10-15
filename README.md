# Aplicación Web de Gestión de Inventario y Almacén - Inti Wasi S.A.C.

Proyecto para la gestión y trazabilidad de inventario, compras y almacén para la Distribuidora Inti Wasi S.A.C.

El frontend integra acceso por roles, usuarios, categorías, proveedores, productos, órdenes de compra, entradas, salidas, ajustes, documentos, movimientos, dashboard y reportes. El Administrador llega al Dashboard y el Operador al listado de órdenes tras iniciar sesión. Ajustes, Dashboard y Reportes están reservados al Administrador; ambos roles consultan Documentos y Movimientos. Los reportes admiten filtros, paginación y exportación PDF o CSV con límite de 10 000 registros. La documentación del Grupo 03 está en `docs/documentos/`.

---

## Requisitos Previos

- **Java:** OpenJDK 25. [Descargar el instalador MSI desde aquí](https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1+1/OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1.msi) y ejecutar la instalación estándar.
- **Base de Datos:** MySQL Server 8.0+
- **IDE:** Visual Studio Code. Se requiere instalar las siguientes extensiones:
  - *Extension Pack for Java* (Microsoft)
  - *Spring Boot Extension Pack* (VMware)
  - *Lombok Annotations Support for VS Code* (Gabriel Basilio)
  - *Thunder Client* (Para pruebas de API REST)
  - *Calidad y entorno visual:* Error Lens, Material Icon Theme.

---

## Guía de Configuración Local

### Nuevas reglas de usuarios

El teléfono es opcional, pero, si se informa, solo admite dígitos, debe ser mayor que cero y no puede repetirse entre usuarios. `01_DDL_bd_intiwasi.sql` incluye `UNIQUE (Telefono)` para las instalaciones nuevas. Los correos creados o editados en la aplicación usan `admin.nombre@intiwasi.pe` u `operador.nombre@intiwasi.pe`, según el rol: una sola palabra con letras minúsculas, sin números ni otros separadores. Si el correo ya está ocupado, se puede añadir la inicial del apellido al nombre, sin separadores. La aplicación también impide dejar el sistema sin un administrador activo.

El JWT dura ocho horas por defecto (`28800000` milisegundos). Si existe una configuración local de `JWT_EXPIRACION_MS` o `jwt.expiracion-ms`, actualízala para usar esa duración. El navegador avisa después de 15 minutos sin actividad y cierra la sesión 30 segundos después si nadie confirma; confirmar no prolonga la vigencia del JWT.

Para reconstruir la base desde cero, ejecuta los cuatro scripts originales en orden. Si necesitas conservar los datos actuales, respáldalos; para aplicar la regla del teléfono, revisa que no existan duplicados antes de ejecutar `ALTER TABLE Usuarios ADD CONSTRAINT UQ_Usuarios_Telefono UNIQUE (Telefono);`. No ejecutes `01_DDL_bd_intiwasi.sql` sobre una base con datos que debas conservar.

### Reglas de inventario incorporadas

Cada guía de remisión identifica una entrega del proveedor. `GuiasRemision` impide repetir el número para el mismo proveedor y una rectificación conserva la guía original. `Productos.DesactivadoPorCategoria` permite reactivar solo los productos que la desactivación de la categoría apagó; los que ya estaban inactivos permanecen así. En Salidas, el motivo «Otro» exige observaciones y una rectificación conserva exactamente los mismos productos, aunque cambien las cantidades.

Los cuatro scripts maestros contienen estas reglas para instalaciones nuevas. Para actualizar una base **con datos**, haz primero un respaldo, comprueba si hay guías duplicadas por proveedor y adapta el esquema conservando las entradas existentes: crea `GuiasRemision`, relaciona cada entrada con su guía, incorpora `DesactivadoPorCategoria`, actualiza la vista Kardex y recrea los triggers con una cuenta MySQL que pueda hacerlo. No ejecutes el script 01 sobre datos que debas conservar. Los productos desactivados antes de incorporar el nuevo campo no se reactivan automáticamente porque no es posible saber cuáles desactivó la categoría.

### 1. Base de Datos (MySQL)

**Aviso:** `01_DDL_bd_intiwasi.sql` elimina y reconstruye `bd_intiwasi`. Haz un respaldo antes de ejecutarlo si necesitas conservar los datos.

1. Abrir MySQL Workbench.
2. Ejecutar los scripts ubicados en la carpeta `database/` en este orden estricto:
   - `01_DDL_bd_intiwasi.sql`
   - `02_Triggers_bd_intiwasi.sql`
   - `03_DML_bd_intiwasi.sql`
   - `04_Vistas_bd_intiwasi.sql`

3. Crear el usuario de desarrollo local (recomendado):
   ```sql
   CREATE USER 'inti'@'localhost' IDENTIFIED BY 'TU_PASSWORD_LOCAL';
   GRANT ALL PRIVILEGES ON bd_intiwasi.* TO 'inti'@'localhost';
   FLUSH PRIVILEGES;
   ```

---

### 2. Configuración del Backend (Spring Boot)

1. Dirigirse a la ruta:
   ```text
   backend/src/main/resources/
   ```
2. Crear una copia del archivo `application-local.properties.example` y nombrarla exactamente:
   ```text
   application-local.properties
   ```
3. Abrir `application-local.properties` y verificar que el `username` y `password` coincidan con tus credenciales locales de MySQL:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/bd_intiwasi?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true
   spring.datasource.username=inti
   spring.datasource.password=PASSWORD_LOCAL
   jwt.secreto=TU_CLAVE_SECRETA_JWT_LOCAL
   ```
   *(Nota: Este archivo está protegido en `.gitignore` y nunca se subirá al repositorio).*
   *Para generar la clave jwt, en powershell copiar el siguiente comando*
   *[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))*
   *Copia esa clave generada y pégalo en jwt.sercreto.*
---

### 3. Ejecución del Proyecto

1. Abrir una terminal en la carpeta `backend/`.
2. Ejecutar el siguiente comando:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. El backend compilará y levantará en:
   ```text
   http://localhost:8080
   ```

### 4. Frontend Angular

En otra terminal, ejecutar desde `frontend/`:

```powershell
npm ci
npm start
```

Abrir `http://localhost:4200`. El servidor Angular redirige las solicitudes `/api` al backend local en el puerto 8080. La aplicación incluye login, sesión, permisos, gestión de usuarios, órdenes de compra, productos, proveedores, dashboard y reportes. Consultar [la guía del frontend](frontend/README.md).
