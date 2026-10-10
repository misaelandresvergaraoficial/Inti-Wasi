const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { Browser, Builder, By, until } = require('selenium-webdriver');

const baseUrl = process.env.E2E_BASE_URL || 'http://localhost:4200';
const email = process.env.E2E_ADMIN_EMAIL?.trim();
const password = process.env.E2E_ADMIN_PASSWORD;
const screenshots = path.resolve(__dirname, '../../screenshots/semana08');

async function saveScreenshot(element, filename) {
  const image = await element.takeScreenshot(true);
  await fs.mkdir(screenshots, { recursive: true });
  await fs.writeFile(path.join(screenshots, filename), Buffer.from(image, 'base64'));
}

async function run() {
  if (!email || !password) {
    throw new Error('Configura E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD para ejecutar la prueba.');
  }

  const driver = await new Builder().forBrowser(Browser.EDGE).build();
  try {
    await driver.manage().window().setRect({ width: 1280, height: 900 });
    await driver.get(new URL('/login', baseUrl).toString());

    const title = await driver.wait(until.elementLocated(By.id('login-title')), 10000);
    await driver.wait(until.elementIsVisible(title), 10000);
    assert.equal(await title.getText(), 'Iniciar sesión');

    const emailField = await driver.findElement(By.id('login-email'));
    const passwordField = await driver.findElement(By.id('login-password'));
    assert.equal(await emailField.isDisplayed(), true);
    assert.equal(await passwordField.isDisplayed(), true);
    await saveScreenshot(await driver.findElement(By.css('.login-card')), '01_formulario_login.png');

    await emailField.sendKeys(email);
    await passwordField.sendKeys(password);
    await driver.findElement(By.css('.login-submit')).click();

    await driver.wait(async () => {
      if (new URL(await driver.getCurrentUrl()).pathname === '/dashboard') return true;
      const alerts = await driver.findElements(By.css('.login-card [role="alert"]'));
      if (alerts.length) throw new Error('El sistema rechazó el inicio de sesión.');
      return false;
    }, 15000);
    const dashboardTitle = await driver.wait(until.elementLocated(By.id('dashboard-title')), 10000);
    await driver.wait(until.elementIsVisible(dashboardTitle), 10000);
    assert.equal(await dashboardTitle.getText(), 'Dashboard');
    await saveScreenshot(await driver.findElement(By.css('.page-heading')), '02_pagina_dashboard.png');

    console.log('APROBADA: el administrador inició sesión y abrió el Dashboard.');
    console.log(`Capturas: ${screenshots}`);
  } finally {
    await driver.quit();
  }
}

run().catch((error) => {
  console.error(`FALLIDA: ${error.message}`);
  process.exitCode = 1;
});
