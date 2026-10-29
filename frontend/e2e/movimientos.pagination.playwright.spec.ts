import { test, expect } from '@playwright/test';

test('CP-T3-PAG: navega entre páginas del historial de movimientos', async ({ page }) => {
  const email = process.env['E2E_ADMIN_EMAIL']?.trim();
  const password = process.env['E2E_ADMIN_PASSWORD'];
  test.skip(!email || !password, 'Configura E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD.');

  await page.goto('/login');
  await page.locator('#login-email').fill(email!);
  await page.locator('#login-password').fill(password!);
  await page.locator('.login-submit').click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.goto('/movimientos');
  const paginator = page.locator('mat-paginator');
  await expect(paginator).toBeVisible();

  const range = paginator.locator('.mat-mdc-paginator-range-label');
  const firstPage = await range.textContent();
  await paginator.getByRole('button', { name: /next page/i }).click();
  await expect(range).not.toHaveText(firstPage ?? '');
});
