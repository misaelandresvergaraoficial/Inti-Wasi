import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: '*.playwright.spec.ts',
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env['E2E_BASE_URL'] || 'http://localhost:4200',
    channel: 'msedge',
    headless: false,
    viewport: { width: 1280, height: 900 },
  },
  expect: { timeout: 10000 },
  timeout: 30000,
});
