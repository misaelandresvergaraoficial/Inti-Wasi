# Aplicación Web de Gestión de Inventario y Almacén - Inti Wasi S.A.C.

Proyecto para la gestión y trazabilidad de inventario, compras y almacén para la Distribuidora Inti Wasi S.A.C.

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

### 1. Base de Datos (MySQL)

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
