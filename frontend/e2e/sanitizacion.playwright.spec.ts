import { expect, test } from '@playwright/test';
import { evidencia } from './support/evidencia';
import { iniciarSesion } from './support/sesion';

test.describe('Sanitización de datos en formularios', () => {
  test.beforeEach(async ({ page }) => {
    await iniciarSesion(page, 'admin');
  });

  test('CP-06 · rechaza un nombre con números', async ({ page }) => {
    await page.goto('/usuarios/nuevo');
    await page.locator('#user-name').fill('Qa Prueba 2026');
    await page.locator('#user-name').blur();

    await expect(
      page.getByText('Revisa el nombre de usuario: contiene caracteres no permitidos.'),
    ).toBeVisible();

    await evidencia(page, {
      id: 'CP-06',
      rf: 'RF03',
      titulo: 'Nombre con números rechazado',
      resultado: 'aprobado',
    });
  });

  test('CP-07 · rechaza un nombre con emoji', async ({ page }) => {
    await page.goto('/usuarios/nuevo');
    await page.locator('#user-name').fill('Qa Prueba \u{1F600}');
    await page.locator('#user-name').blur();

    await expect(
      page.getByText('Revisa el nombre de usuario: contiene caracteres no permitidos.'),
    ).toBeVisible();

    await evidencia(page, {
      id: 'CP-07',
      rf: 'RF03',
      titulo: 'Nombre con emoji rechazado',
      resultado: 'aprobado',
    });
  });

  test('CP-08 · rechaza un teléfono de 8 dígitos', async ({ page }) => {
    await page.goto('/usuarios/nuevo');
    await page.locator('#user-phone').fill('98765432');
    await page.locator('#user-phone').blur();

    await expect(
      page.getByText('El teléfono debe tener 9 dígitos y comenzar con 9 (ej. 979348552).'),
    ).toBeVisible();

    await evidencia(page, {
      id: 'CP-08',
      rf: 'RF03',
      titulo: 'Teléfono de 8 dígitos rechazado',
      resultado: 'aprobado',
    });
  });

  test('CP-09 · rechaza un precio negativo', async ({ page }) => {
    await page.goto('/productos/nuevo');
    const precio = page.getByLabel('Precio de compra referencial (S/)');
    await expect(precio).toBeVisible();
    await precio.fill('-1');
    await precio.blur();

    await expect(page.getByText('No puede ser negativo.')).toBeVisible();

    await evidencia(page, {
      id: 'CP-09',
      rf: 'RF06',
      titulo: 'Precio negativo rechazado',
      resultado: 'aprobado',
    });
  });

  test('CP-10 · rechaza una guía con prefijo de otro proveedor', async ({ page }) => {
    await page.goto('/entradas/nueva');

    const orden = page.locator('#entrada-orden');
    await expect(orden).toBeVisible();
    const opciones = orden.locator('option');
    expect(await opciones.count()).toBeGreaterThan(1);
    await orden.selectOption({ index: 1 });

    await expect(page.locator('#entrada-guia-ayuda')).toContainText('Prefijo obligatorio:');
    await page.locator('#entrada-guia').fill('GUIA-AJENA-001-000001');
    await page.locator('#entrada-guia').blur();

    await expect(page.locator('#entrada-guia-error')).toContainText(
      'La guía no corresponde al proveedor de la orden',
    );

    await evidencia(page, {
      id: 'CP-10',
      rf: 'RF08',
      titulo: 'Guía con prefijo ajeno rechazada',
      resultado: 'aprobado',
    });
  });
});
