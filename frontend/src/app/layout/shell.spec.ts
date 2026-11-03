import { Component, computed, signal } from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../core/auth.service';
import { Shell } from './shell';

@Component({ template: '<h1>Productos</h1>' })
class TestPage {}

afterEach(() => TestBed.resetTestingModule());

async function setup(admin = true) {
  const isAdmin = signal(admin);
  const breakpoints = new BehaviorSubject({ matches: false, breakpoints: {} });
  TestBed.configureTestingModule({
    imports: [Shell],
    providers: [
      provideRouter([{ path: 'productos', component: TestPage, title: 'Productos · Inti Wasi' }]),
      { provide: BreakpointObserver, useValue: { observe: () => breakpoints } },
      {
        provide: AuthService,
        useValue: {
          isAdmin,
          session: computed(() => ({
            nombre: 'Ana O’Neill',
            rol: isAdmin() ? 'Administrador' : 'Operador de Almacén',
          })),
          landingRoute: () => (isAdmin() ? '/dashboard' : '/ordenes-compra'),
          logout: vi.fn(),
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(Shell);
  fixture.detectChanges();
  await fixture.whenStable();
  return { fixture, isAdmin, breakpoints, element: fixture.nativeElement as HTMLElement };
}

describe('Navegación por rol', () => {
  it('muestra doce módulos al administrador y siete al operador sin enlaces administrativos', async () => {
    const { fixture, element, isAdmin } = await setup();
    expect(element.querySelectorAll('nav a')).toHaveLength(12);
    isAdmin.set(false);
    fixture.detectChanges();
    expect(element.querySelectorAll('nav a')).toHaveLength(7);
    for (const path of ['/dashboard', '/usuarios', '/categorias', '/ajustes', '/reportes']) {
      expect(element.querySelector(`nav a[href="${path}"]`)).toBeNull();
    }
  });

  it('actualiza ubicación y página activa al navegar, sin cambiar la URL por el salto al contenido', async () => {
    const { fixture, element } = await setup(false);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/productos');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(element.querySelector('.location strong')?.textContent).toBe('Productos');
    expect(element.querySelector('nav a[aria-current="page"]')?.getAttribute('href')).toBe(
      '/productos',
    );
    element.querySelector<HTMLAnchorElement>('.skip-link')!.click();
    expect(router.url).toBe('/productos');
  });

  it('cierra el menú compacto con Escape y al volver a escritorio', async () => {
    const { fixture, breakpoints, element } = await setup(false);
    breakpoints.next({ matches: true, breakpoints: {} });
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.menu-toggle')!.click();
    fixture.detectChanges();
    expect(element.querySelector('.sidebar')?.getAttribute('aria-modal')).toBe('true');
    expect(element.querySelector('.sidebar')?.hasAttribute('inert')).toBe(false);
    element
      .querySelector('.app-layout')!
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.menuOpen()).toBe(false);
    expect(element.querySelector('.sidebar')?.hasAttribute('inert')).toBe(true);
    expect(document.activeElement).toBe(element.querySelector('.menu-toggle'));
    fixture.componentInstance.openMenu();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.menu-close')!.focus();
    element.querySelector<HTMLButtonElement>('.menu-close')!.blur();
    breakpoints.next({ matches: false, breakpoints: {} });
    fixture.detectChanges();
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
    expect(fixture.componentInstance.menuOpen()).toBe(false);
    expect(element.querySelector('.sidebar')?.hasAttribute('inert')).toBe(false);
    expect(document.activeElement).toBe(element.querySelector('.sidebar a'));
  });
});
