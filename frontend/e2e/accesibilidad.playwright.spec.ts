import { promises as fs } from 'node:fs';
import path from 'node:path';
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { evidencia } from './support/evidencia';
import { iniciarSesion } from './support/sesion';

interface ResumenAxe {
  pagina: string;
  url: string;
  violaciones: Array<{
    id: string;
    impacto: string | null;
    descripcion: string;
    ayuda: string;
    nodos: number;
  }>;
}

async function auditar(page: Page, id: string, pagina: string, titulo: string): Promise<void> {
  const resultado = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  const resumen: ResumenAxe = {
    pagina,
    url: page.url(),
    violaciones: resultado.violations.map((violacion) => ({
      id: violacion.id,
      impacto: violacion.impact,
      descripcion: violacion.description,
      ayuda: violacion.help,
      nodos: violacion.nodes.length,
      objetivos: violacion.nodes.map((nodo) => ({
        selector: nodo.target.join(' '),
        html: nodo.html,
      })),
    })),
  };

  const fecha = new Date().toISOString().slice(0, 10);
  const carpeta = path.resolve(__dirname, '../../docs/evidencias/e2e', fecha);
  await fs.mkdir(carpeta, { recursive: true });
  await fs.writeFile(
    path.join(carpeta, `axe-${pagina}.json`),
    `${JSON.stringify(resumen, null, 2)}\n`,
    'utf8',
  );

  const graves = resumen.violaciones.filter(
    (violacion) => violacion.impacto === 'critical' || violacion.impacto === 'serious',
  );

  await evidencia(page, {
    id,
    rf: 'RNF08',
    titulo,
    resultado: graves.length === 0 ? 'aprobado' : 'fallido',
  });

  expect(
    graves,
    `Violaciones graves en ${pagina}: ${JSON.stringify(graves, null, 2)}`,
  ).toHaveLength(0);
}

test.describe('Accesibilidad con axe-core', () => {
  test('CP-24 · login sin violaciones graves', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('#login-title')).toBeVisible();

    await auditar(page, 'CP-24', 'login', 'Accesibilidad del login');
  });

  test('CP-25 · dashboard sin violaciones graves', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await expect(page.locator('#dashboard-title')).toBeVisible();

    await auditar(page, 'CP-25', 'dashboard', 'Accesibilidad del dashboard');
  });

  test('CP-26 · productos sin violaciones graves', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/productos');
    await expect(page.getByRole('heading', { name: 'Productos', exact: true })).toBeVisible();

    await auditar(page, 'CP-26', 'productos', 'Accesibilidad de productos');
  });

  test('CP-27 · movimientos sin violaciones graves', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/movimientos');
    await page.getByRole('button', { name: 'Consultar' }).click();
    await expect(page.locator('mat-paginator')).toBeVisible();

    await auditar(page, 'CP-27', 'movimientos', 'Accesibilidad de movimientos');
  });
});
