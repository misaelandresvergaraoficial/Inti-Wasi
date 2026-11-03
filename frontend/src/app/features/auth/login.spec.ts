import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { Login } from './login';

afterEach(() => TestBed.resetTestingModule());

function setup() {
  const login = vi.fn();
  TestBed.configureTestingModule({
    imports: [Login],
    providers: [
      provideRouter([]),
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap({ sesion: 'inactiva' }) } },
      },
      { provide: AuthService, useValue: { login, landingRoute: () => '/ordenes-compra' } },
    ],
  });
  const fixture = TestBed.createComponent(Login);
  fixture.detectChanges();
  return { fixture, login };
}

describe('Acceso del equipo', () => {
  it('rechaza formulario vacío, permite mostrar contraseña y explica el cierre por inactividad', async () => {
    const { fixture, login } = setup();
    await fixture.componentInstance.submit();
    expect(login).not.toHaveBeenCalled();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('se cerró por inactividad');
    element.querySelector<HTMLButtonElement>('[aria-label="Mostrar contraseña"]')!.click();
    fixture.detectChanges();
    expect(element.querySelector<HTMLInputElement>('#login-password')?.type).toBe('text');
    expect(
      element.querySelector('[aria-label="Ocultar contraseña"]')?.getAttribute('aria-pressed'),
    ).toBe('true');
  });

  it('evita doble envío y conserva la contraseña literal antes de dirigir al destino del rol', async () => {
    const { fixture, login } = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    let resolve!: () => void;
    login.mockReturnValue(new Promise<void>((done) => (resolve = done)));
    fixture.componentInstance.form.setValue({
      correo: 'operador.ana@intiwasi.pe',
      contrasena: ' Abcdé ',
    });
    const first = fixture.componentInstance.submit();
    await fixture.componentInstance.submit();
    expect(login).toHaveBeenCalledExactlyOnceWith('operador.ana@intiwasi.pe', ' Abcdé ');
    expect(fixture.componentInstance.busy()).toBe(true);
    resolve();
    await first;
    expect(navigate).toHaveBeenCalledWith(['/ordenes-compra']);
    expect(fixture.componentInstance.busy()).toBe(false);
  });
});
