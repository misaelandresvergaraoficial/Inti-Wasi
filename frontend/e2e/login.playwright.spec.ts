import { test, expect } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
import path from 'node:path';

const screenshots = path.resolve(__dirname, '../../screenshots/semana08/playwright');

test('administrador inicia sesión y abre Dashboard', async ({ page }) => {
  const email = process.env['E2E_ADMIN_EMAIL']?.trim();
  const password = process.env['E2E_ADMIN_PASSWORD'];
  if (!email || !password) {
    throw new Error('Configura E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD para ejecutar la prueba.');
  }

  await mkdir(screenshots, { recursive: true });
  await page.goto('/login');
  await expect(page.locator('#login-title')).toHaveText('Iniciar sesión');
  await expect(page.locator('#login-email')).toBeVisible();
  await expect(page.locator('#login-password')).toBeVisible();
  const formImage = path.join(screenshots, '01_formulario_login.png');
  await page.locator('.login-card').screenshot({ path: formImage });
  await test.info().attach('Formulario antes de escribir datos', {
    path: formImage,
    contentType: 'image/png',
  });

  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('.login-submit').click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.locator('#dashboard-title')).toHaveText('Dashboard');
  const dashboardImage = path.join(screenshots, '02_pagina_dashboard.png');
  await page.locator('.page-heading').screenshot({ path: dashboardImage });
  await test.info().attach('Página Dashboard', { path: dashboardImage, contentType: 'image/png' });
});
