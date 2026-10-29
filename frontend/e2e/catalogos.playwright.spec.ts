import { expect, test } from '@playwright/test';
import { evidencia } from './support/evidencia';
import { iniciarSesion } from './support/sesion';

function marcaTemporal(): string {
  return Date.now().toString().slice(-5);
}

function rucAleatorio(): string {
  let resto = '';
  for (let i = 0; i < 9; i += 1) {
    resto += Math.floor(Math.random() * 10);
  }
  return `20${resto}`;
}

test.describe('Gestión de catálogos', () => {
  test.beforeEach(async ({ page }) => {
    await iniciarSesion(page, 'admin');
  });

  test('CP-11 · crea, edita y desactiva una categoría', async ({ page }) => {
    const id = marcaTemporal();
    const nombre = `QA Categoria ${id}`;
    const nombreEditado = `${nombre} Renombrada`;

    await page.goto('/categorias/nuevo');
    await page.locator('input[formcontrolname="nomCategoria"]').fill(nombre);
    await page.getByRole('button', { name: 'Crear categoría' }).click();
    await expect(page).toHaveURL(/\/categorias$/);
    await expect(page.getByRole('status').filter({ hasText: 'categoría creada' })).toBeVisible();

    await page.locator('#categoria-search').fill(nombre);
    const fila = page.getByRole('row').filter({ hasText: nombre });
    await expect(fila).toBeVisible();

    await fila.getByRole('link', { name: `Editar ${nombre}` }).click();
    await expect(page).toHaveURL(/\/categorias\/\d+\/editar$/);
    await page.locator('input[formcontrolname="nomCategoria"]').fill(nombreEditado);
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page).toHaveURL(/\/categorias$/);
    await expect(page.getByRole('status').filter({ hasText: 'cambios guardados' })).toBeVisible();

    await page.locator('#categoria-search').fill(nombreEditado);
    const filaEditada = page.getByRole('row').filter({ hasText: nombreEditado });
    await expect(filaEditada).toBeVisible();
    await filaEditada.getByRole('button', { name: `Desactivar ${nombreEditado}` }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Desactivar categoría', exact: true })
      .click();
    await expect(page.getByRole('status').filter({ hasText: /desactiva/i })).toBeVisible();

    await evidencia(page, {
      id: 'CP-11',
      rf: 'RF04',
      titulo: 'Categoría creada, editada y desactivada',
      resultado: 'aprobado',
    });
  });

  test('CP-12 · crea un proveedor con razón social y RUC válidos', async ({ page }) => {
    const id = marcaTemporal();
    const nombre = `QA Proveedor ${id} S.A.C.`;

    await page.goto('/proveedores/nuevo');
    await page.locator('#supplier-name').fill(nombre);
    await page.locator('#supplier-ruc').fill(rucAleatorio());
    await page.locator('#supplier-prefix').fill(`GUIA-QA${id}-`);
    await page.locator('#supplier-contact').fill('Contacto de Prueba');
    await page.locator('#supplier-phone').fill('014150000');
    await page.getByRole('button', { name: 'Crear proveedor' }).click();

    await expect(page).toHaveURL(/\/proveedores$/);
    await page.locator('#supplier-search').fill(nombre);
    await expect(page.getByRole('row').filter({ hasText: nombre })).toBeVisible();

    await evidencia(page, {
      id: 'CP-12',
      rf: 'RF05',
      titulo: 'Proveedor creado correctamente',
      resultado: 'aprobado',
    });
  });

  test('CP-13 · crea un producto con categoría y proveedor', async ({ page }) => {
    const id = marcaTemporal();
    const nombre = `QA Monitor LED 24 (3 m) ${id}`;

    await page.goto('/productos/nuevo');
    await page.locator('input[formcontrolname="sku"]').fill(`QA-${id}`);
    await page.getByRole('combobox', { name: 'Categoría' }).click();
    await page.getByRole('option', { name: 'Procesadores' }).click();
    await page.locator('input[formcontrolname="nomProducto"]').fill(nombre);
    await page.getByRole('combobox', { name: 'Proveedor (opcional)' }).click();
    await page.getByRole('option', { name: 'Grupo Deltron S.A.' }).click();
    await page.getByLabel('Precio de compra referencial (S/)').fill('125.50');
    await page.locator('input[formcontrolname="stockMinimo"]').fill('2');
    await page.getByRole('button', { name: 'Crear producto' }).click();

    await expect(page).toHaveURL(/\/productos$/);
    await page.locator('#producto-search').fill(nombre);
    await expect(page.getByRole('row').filter({ hasText: nombre })).toBeVisible();

    await evidencia(page, {
      id: 'CP-13',
      rf: 'RF06',
      titulo: 'Producto creado con categoría y proveedor',
      resultado: 'aprobado',
    });
  });

  test('CP-14 · rechaza una categoría duplicada', async ({ page }) => {
    const id = marcaTemporal();
    const nombre = `QA Duplicada ${id}`;

    await page.goto('/categorias/nuevo');
    await page.locator('input[formcontrolname="nomCategoria"]').fill(nombre);
    await page.getByRole('button', { name: 'Crear categoría' }).click();
    await expect(page).toHaveURL(/\/categorias$/);

    await page.goto('/categorias/nuevo');
    await page.locator('input[formcontrolname="nomCategoria"]').fill(nombre);
    await page.getByRole('button', { name: 'Crear categoría' }).click();

    await expect(page.getByRole('alert')).toContainText('Ya existe una categoría');
    await expect(page).toHaveURL(/\/categorias\/nuevo$/);

    await evidencia(page, {
      id: 'CP-14',
      rf: 'RF04',
      titulo: 'Categoría duplicada rechazada',
      resultado: 'aprobado',
    });
  });
});
