import { expect, test, type Locator, type Page } from '@playwright/test';
import { evidencia } from './support/evidencia';
import { iniciarSesion } from './support/sesion';

test.describe.configure({ mode: 'serial' });

const SKU = 'PRO-AMD-5600X';
let ordenId = 0;
let guia = '';

async function esperarOpciones(select: Locator, minimo = 2): Promise<void> {
  await expect
    .poll(() => select.locator('option').count(), { timeout: 15000 })
    .toBeGreaterThanOrEqual(minimo);
}

async function elegirOpcion(select: Locator, texto: string): Promise<string> {
  await esperarOpciones(select);
  const opciones = select.locator('option');
  const total = await opciones.count();
  const textos: string[] = [];

  for (let i = 0; i < total; i += 1) {
    const contenido = (await opciones.nth(i).textContent()) ?? '';
    textos.push(contenido.trim());
    if (contenido.includes(texto)) {
      await select.selectOption({ index: i });
      return contenido;
    }
  }

  throw new Error(
    `No se encontró la opción que contiene "${texto}". Opciones: ${JSON.stringify(textos)}`,
  );
}

async function idDeUrl(page: Page, ruta: string): Promise<number> {
  const coincidencia = page.url().match(new RegExp(`${ruta}/(\\d+)$`));
  expect(coincidencia, `La URL no coincide con ${ruta}/<id>: ${page.url()}`).not.toBeNull();
  return Number(coincidencia![1]);
}

test.describe('Operaciones de almacén', () => {
  test.beforeEach(async ({ page }) => {
    await iniciarSesion(page, 'admin');
  });

  test('CP-15 · registra una orden de compra', async ({ page }) => {
    await page.goto('/ordenes-compra/nueva');
    await page.locator('#order-supplier').selectOption({ label: 'Grupo Deltron S.A.' });

    const producto = page.locator('#line-product-0');
    await esperarOpciones(producto);
    await elegirOpcion(producto, SKU);
    await page.locator('#line-quantity-0').fill('2');
    await page.getByRole('button', { name: 'Crear orden' }).click();

    await expect(page).toHaveURL(/\/ordenes-compra\/\d+$/);
    ordenId = await idDeUrl(page, '/ordenes-compra');
    await expect(page.getByText(/Orden N\.° \d+/).first()).toBeVisible();

    await evidencia(page, {
      id: 'CP-15',
      rf: 'RF07',
      titulo: 'Orden de compra registrada',
      resultado: 'aprobado',
    });
  });

  test('CP-16 · registra una entrada parcial', async ({ page }) => {
    await page.goto('/entradas/nueva');
    const orden = page.locator('#entrada-orden');
    await elegirOpcion(orden, `N.° ${ordenId} ·`);

    const ayuda = page.locator('#entrada-guia-ayuda');
    await expect(ayuda).toContainText('Prefijo obligatorio:');
    const textoAyuda = (await ayuda.textContent()) ?? '';
    const prefijo = textoAyuda.match(/(GUIA-[A-Z][A-Z0-9]{0,23}-)/)?.[1];
    expect(prefijo, `No se pudo leer el prefijo en: ${textoAyuda}`).toBeTruthy();

    guia = `${prefijo}001-${ordenId}01`;
    await page.locator('#entrada-guia').fill(guia);
    await elegirOpcion(page.locator('#entrada-producto-0'), SKU);
    await page.locator('#entrada-cantidad-0').fill('1');
    await page.getByRole('button', { name: 'Registrar entrada' }).click();

    await expect(page).toHaveURL(/\/entradas\/\d+$/);
    await expect(page.getByRole('status').filter({ hasText: /registrad/i })).toBeVisible();

    await evidencia(page, {
      id: 'CP-16',
      rf: 'RF08',
      titulo: 'Entrada parcial registrada',
      resultado: 'aprobado',
    });
  });

  test('CP-17 · rechaza una guía de remisión duplicada', async ({ page }) => {
    await page.goto('/entradas/nueva');
    await elegirOpcion(page.locator('#entrada-orden'), `N.° ${ordenId} ·`);
    await page.locator('#entrada-guia').fill(guia);
    await elegirOpcion(page.locator('#entrada-producto-0'), SKU);
    await page.locator('#entrada-cantidad-0').fill('1');
    await page.getByRole('button', { name: 'Registrar entrada' }).click();

    await expect(page.getByRole('alert')).toContainText(/ya está registrada/i);

    await evidencia(page, {
      id: 'CP-17',
      rf: 'RF08',
      titulo: 'Guía de remisión duplicada rechazada',
      resultado: 'aprobado',
    });
  });

  test('CP-18 · registra una salida de almacén', async ({ page }) => {
    await page.goto('/salidas/nueva');
    await page.locator('#salida-motivo').selectOption('Despacho/Venta');
    await elegirOpcion(page.locator('#salida-producto-0'), SKU);
    await page.locator('#salida-cantidad-0').fill('1');
    await page.getByRole('button', { name: 'Registrar salida' }).click();

    await expect(page).toHaveURL(/\/salidas\/\d+$/);

    await evidencia(page, {
      id: 'CP-18',
      rf: 'RF09',
      titulo: 'Salida de almacén registrada',
      resultado: 'aprobado',
    });
  });

  test('CP-19 · registra un ajuste de inventario', async ({ page }) => {
    await page.goto('/ajustes/nuevo');
    await page.locator('#ajuste-tipo').selectOption('Incremento');
    await page.locator('#ajuste-motivo').fill('Conteo físico QA');
    await elegirOpcion(page.locator('#ajuste-producto-0'), SKU);
    await page.locator('#ajuste-cantidad-0').fill('2');
    await page.getByRole('button', { name: 'Registrar ajuste' }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Registrar ajuste', exact: true })
      .click();

    await expect(page).toHaveURL(/\/ajustes\/\d+$/);

    await evidencia(page, {
      id: 'CP-19',
      rf: 'RF10',
      titulo: 'Ajuste de inventario registrado',
      resultado: 'aprobado',
    });
  });
});
