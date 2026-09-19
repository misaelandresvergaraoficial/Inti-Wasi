# 🎨 Frontend — Distribuidora Inti Wasi S.A.C.

Este módulo contiene la interfaz web de usuario desarrollada con **Angular** y **Tailwind CSS**, estructurada mediante arquitectura **API REST** para conectarse con el backend en **Spring Boot** mediante autenticación segura por **Tokens JWT**.

---

## 📋 1. Requisitos Previos

Antes de ejecutar los comandos en la terminal, asegúrate de tener instalado en tu computadora:

* **Node.js**: Versión LTS (v20 o superior). [Descargar Node.js](https://nodejs.org/)

  * *Verificación en terminal:* `node -v` y `npm -v`
* **VS Code** (o tu editor de código preferido).
* **Git**: Para el control de versiones.

---

## 📥 2. Obtener los Cambios del Repositorio

Abre la terminal en la raíz del proyecto (`Inti-Wasi`) y descarga la rama de trabajo correspondiente:

```bash
# 1. Actualizar referencias del servidor remoto
git fetch origin

# 2. Cambiar a la rama de desarrollo del frontend
git checkout feature/frontend
```

---

## ⚙️ 3. Instalación y Ejecución Paso a Paso

Sigue estos comandos en la terminal para preparar el entorno e iniciar el servidor local de desarrollo:

```bash
# 1. Entrar a la carpeta del frontend
cd frontend

# 2. Instalar todas las dependencias del proyecto
#    (Solo la primera vez o tras un git pull)
npm install

# 3. Iniciar el servidor local de Angular
npx ng serve -o
```

> 💡 **Nota de recomendación:** Usar `npx ng serve -o` evita la necesidad de instalar `@angular/cli` de forma global en tu sistema y abrirá automáticamente la aplicación en tu navegador predeterminado en `http://localhost:4200/`.

---

## 🔍 4. Módulos e Integraciones Configuradas

La estructura actual del proyecto incluye:

* **Tailwind CSS v3:** Configurado para el estilizado mediante utilidades en `styles.css`.

* **HttpClient & Interceptor JWT:** Ubicado en `core/interceptors/jwt.interceptor.ts`. Inyecta automáticamente el encabezado `Authorization: Bearer <token>` a cada petición saliente.

* **Servicio de Autenticación (`AuthService`):** Maneja el inicio de sesión (`POST /api/auth/login`) y el almacenamiento de credenciales en `localStorage`.

* **Protección de Rutas (`AuthGuard`):** Bloquea el acceso directo a `/dashboard` si no existe un token válido almacenado.

* **Layout Base:** Componente principal (`core/components/layout`) con Sidebar lateral oscuro, datos del usuario activo y botón de cierre de sesión.

---

## ⚠️ 5. Solución de Problemas Frecuentes (Troubleshooting)

### 🔴 Error: `Failed to fetch` al intentar iniciar sesión

* **Causa:** El servidor de Angular no logra conectarse con la API REST.
* **Solución:** Asegúrate de tener corriendo en paralelo el servidor Backend (Spring Boot) en la ruta `http://localhost:8080/`.

Ambos servidores deben estar encendidos al mismo tiempo:

| Servidor              | Puerto |
| --------------------- | -----: |
| Backend — Spring Boot | `8080` |
| Frontend — Angular    | `4200` |

### 🔴 Error: `Cannot find module or type declarations for side-effect import of 'zone.js'`

* **Causa:** Falta el motor de detección de cambios de Angular en las dependencias locales.
* **Solución:** Ejecuta el siguiente comando dentro de la carpeta `frontend/`:

```bash
npm install zone.js
```

### 🔴 Advertencias de compilación de Tailwind CSS

* **Causa:** Conflicto de versión si se instala la v4 de Tailwind CSS por defecto.
* **Recomendación:** El proyecto utiliza la versión estable **Tailwind CSS v3**.

Si experimentas errores en la compilación de estilos, reinstala la versión 3 ejecutando:

```bash
npm uninstall tailwindcss postcss autoprefixer
npm install -D tailwindcss@3 postcss autoprefixer
```

---

## 💡 Recomendaciones para el Equipo

* **No subir `node_modules`:** El archivo `.gitignore` interno de la carpeta `frontend` ya está configurado para omitir `node_modules` y la carpeta temporal `.angular`.

* **Regla de ejecución:** Cada vez que descargues cambios del repositorio con `git pull`, ejecuta `npm install` dentro de `frontend/` para asegurarte de tener todas las librerías al día.
