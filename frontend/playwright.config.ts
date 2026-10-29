import { defineConfig } from '@playwright/test';

const executablePath = process.env['E2E_CHROMIUM_PATH'];

export default defineConfig({
  testDir: './e2e',
  testMatch: '*.playwright.spec.ts',
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env['E2E_BASE_URL'] || 'http://localhost:4200',
    channel: process.env['E2E_CHANNEL'] || undefined,
    headless: process.env['E2E_HEADED'] !== 'true',
    launchOptions: executablePath ? { executablePath } : undefined,
    viewport: { width: 1280, height: 900 },
  },
  expect: { timeout: 10000 },
  timeout: 30000,
});
