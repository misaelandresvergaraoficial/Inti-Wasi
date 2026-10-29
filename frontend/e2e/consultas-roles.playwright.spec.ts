import { expect, test } from '@playwright/test';
import { evidencia } from './support/evidencia';
import { iniciarSesion } from './support/sesion';

test.describe('Consultas y control por roles', () => {
  test('CP-20 · consulta el listado de documentos', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/documentos');

    await expect(page.getByRole('heading', { name: 'Documentos', exact: true })).toBeVisible();
    await expect(page.locator('#documento-search')).toBeVisible();
    await expect(page.getByRole('region', { name: 'Listado de documentos' })).toBeVisible();

    await evidencia(page, {
      id: 'CP-20',
      rf: 'RF12',
      titulo: 'Consulta de documentos',
      resultado: 'aprobado',
    });
  });

  test('CP-21 · pagina el historial de movimientos', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/movimientos');
    await page.getByRole('button', { name: 'Consultar' }).click();

    const paginador = page.locator('mat-paginator');
    await expect(paginador).toBeVisible();
    const rango = paginador.locator('.mat-mdc-paginator-range-label');
    await expect(rango).not.toHaveText(/^1\s*[–-]\s*0/);
    const primeraPagina = (await rango.textContent()) ?? '';

    const siguiente = paginador.getByRole('button', { name: /next page/i });
    if (await siguiente.isEnabled()) {
      await siguiente.click();
      await expect(rango).not.toHaveText(primeraPagina);
    }

    await expect(page.getByText(/movimientos con estos filtros/)).toBeVisible();

    await evidencia(page, {
      id: 'CP-21',
      rf: 'RF12',
      titulo: 'Movimientos paginados',
      resultado: 'aprobado',
    });
  });

  test('CP-22 · muestra el dashboard administrativo', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await expect(page.locator('#dashboard-title')).toHaveText('Dashboard');
    await expect(page.locator('#activity-title')).toBeVisible();
    await expect(page.locator('#reponer-title')).toBeVisible();

    await evidencia(page, {
      id: 'CP-22',
      rf: 'RF13',
      titulo: 'Dashboard con indicadores',
      resultado: 'aprobado',
    });
  });

  test('CP-23 · el Operador no accede a administración', async ({ page }) => {
    await iniciarSesion(page, 'operador');
    await expect(page.getByRole('link', { name: 'Usuarios' })).toHaveCount(0);

    await page.goto('/usuarios');
    await expect(page).toHaveURL(/\/acceso-denegado$/);
    await expect(
      page.getByRole('heading', { name: 'Esta sección requiere otro permiso' }),
    ).toBeVisible();

    await evidencia(page, {
      id: 'CP-23',
      rf: 'RF02',
      titulo: 'Acceso denegado al Operador',
      resultado: 'aprobado',
    });
  });
});
