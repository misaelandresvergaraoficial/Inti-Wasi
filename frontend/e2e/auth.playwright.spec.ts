import { expect, test } from '@playwright/test';
import { evidencia } from './support/evidencia';
import {
  cerrarSesion,
  credenciales,
  iniciarSesion,
  intentarAcceso,
  nombreQa,
  telefonoQa,
} from './support/sesion';

test.describe('Autenticación y control de acceso', () => {
  test('CP-01 · inicia sesión con credenciales válidas', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await expect(page.locator('#dashboard-title')).toHaveText('Dashboard');

    await evidencia(page, {
      id: 'CP-01',
      rf: 'RF01',
      titulo: 'Inicio de sesión válido',
      resultado: 'aprobado',
    });
  });

  test('CP-02 · rechaza una contraseña incorrecta', async ({ page }) => {
    const { email, password } = credenciales('admin');
    await intentarAcceso(page, email, `${password}incorrecta`);

    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByRole('alert')).toContainText(/incorrect/i);
    await expect(page).toHaveURL(/\/login$/);

    await evidencia(page, {
      id: 'CP-02',
      rf: 'RF01',
      titulo: 'Contraseña incorrecta rechazada',
      resultado: 'aprobado',
    });
  });

  test('CP-04 · el guard redirige al login sin sesión', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.locator('#login-title')).toHaveText('Iniciar sesión');

    await evidencia(page, {
      id: 'CP-04',
      rf: 'RF02',
      titulo: 'Acceso sin sesión redirigido al login',
      resultado: 'aprobado',
    });
  });

  test('CP-05 · cierra sesión y bloquea el acceso posterior', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await cerrarSesion(page);

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login$/);

    await evidencia(page, {
      id: 'CP-05',
      rf: 'RF15',
      titulo: 'Cierre de sesión efectivo',
      resultado: 'aprobado',
    });
  });

  test('CP-03 · rechaza el acceso de una cuenta inactiva', async ({ page }) => {
    test.setTimeout(90000);

    const nombre = nombreQa('Qa Inactivo');
    const correo = nombreQa('qa').replace(/\s+/g, '');
    const telefono = telefonoQa();
    const clave = 'Qa2026prueba';

    await iniciarSesion(page, 'admin');

    await page.goto('/usuarios/nuevo');
    await page.locator('#user-name').fill(nombre);
    await page.locator('#user-email').fill(`operador.${correo}`);
    await page.locator('#user-password').fill(clave);
    await page.locator('#user-phone').fill(telefono);
    await page.getByRole('button', { name: 'Crear usuario' }).click();
    await expect(page).toHaveURL(/\/usuarios$/);

    const fila = page.getByRole('row').filter({ hasText: nombre });
    await page.locator('#user-search').fill(nombre);
    await expect(fila).toBeVisible();
    await fila.getByRole('button', { name: `Desactivar a ${nombre}` }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Desactivar usuario', exact: true })
      .click();
    await expect(page.getByRole('status').filter({ hasText: 'fue desactivado' })).toBeVisible();

    await cerrarSesion(page);
    await intentarAcceso(page, `operador.${correo}@intiwasi.pe`, clave);

    await expect(page.getByRole('alert')).toContainText(/no está disponible/i);
    await expect(page).toHaveURL(/\/login$/);

    await evidencia(page, {
      id: 'CP-03',
      rf: 'RF01',
      titulo: 'Cuenta inactiva rechazada',
      resultado: 'aprobado',
    });
  });
});
