import { expect, type Page } from '@playwright/test';

export type Perfil = 'admin' | 'operador';

export interface Credenciales {
  email: string;
  password: string;
}

export function credenciales(perfil: Perfil): Credenciales {
  const email =
    perfil === 'admin'
      ? process.env['E2E_ADMIN_EMAIL']?.trim()
      : process.env['E2E_OPERADOR_EMAIL']?.trim();
  const password =
    perfil === 'admin' ? process.env['E2E_ADMIN_PASSWORD'] : process.env['E2E_OPERADOR_PASSWORD'];

  if (!email || !password) {
    throw new Error(
      `Configura las credenciales E2E del perfil ${perfil} en frontend/.env antes de ejecutar.`,
    );
  }

  return { email, password };
}

/** Genera un nombre de persona único, solo con letras y espacios. */
export function nombreQa(prefijo = 'Qa Prueba'): string {
  const letras = 'abcdefghijklmnopqrstuvwxyz';
  let cola = '';
  for (let i = 0; i < 6; i += 1) {
    cola += letras[Math.floor(Math.random() * letras.length)];
  }
  return `${prefijo} ${cola}`;
}

/** Genera un teléfono de 9 dígitos que empieza con 9. */
export function telefonoQa(): string {
  let resto = '';
  for (let i = 0; i < 8; i += 1) {
    resto += Math.floor(Math.random() * 10);
  }
  return `9${resto}`;
}

/** Inicia sesión por la interfaz y espera la pantalla inicial del rol. */
export async function iniciarSesion(page: Page, perfil: Perfil = 'admin'): Promise<Credenciales> {
  const datos = credenciales(perfil);

  await page.goto('/login');
  await expect(page.locator('#login-title')).toHaveText('Iniciar sesión');
  await page.locator('#login-email').fill(datos.email);
  await page.locator('#login-password').fill(datos.password);
  await page.locator('.login-submit').click();

  await expect(page).toHaveURL(perfil === 'admin' ? /\/dashboard$/ : /\/ordenes-compra$/);
  return datos;
}

/** Intenta iniciar sesión sin esperar una pantalla válida. */
export async function intentarAcceso(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('.login-submit').click();
}

export async function cerrarSesion(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page).toHaveURL(/\/login$/);
}
