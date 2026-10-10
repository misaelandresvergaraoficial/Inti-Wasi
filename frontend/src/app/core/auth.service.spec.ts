import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { UsuarioResponse } from './models';

const user: UsuarioResponse = {
  idUsuario: 6,
  nomUsuario: 'Andrés Vega',
  correo: 'andres@intiwasi.test',
  rol: 'Operador de Almacén',
  telefono: null,
  estado: 1,
  fechaRegistro: '2026-09-01T09:00:00',
};
const expiry = Math.floor(Date.now() / 1000) + 3600;
const token = `header.${btoa(JSON.stringify({ exp: expiry }))}.signature`;

describe('Sesión conectada al backend', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('envía las credenciales y consulta la identidad real antes de abrir la sesión', async () => {
    const login = auth.login(user.correo, 'correcta');
    const request = http.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.body).toEqual({ correo: user.correo, contrasena: 'correcta' });
    request.flush({ token, correo: user.correo, rol: 'ROLE_Operador de Almacén' });
    await Promise.resolve();
    const current = http.expectOne('/api/auth/me');
    expect(current.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    current.flush(user);
    await login;

    expect(auth.session()?.nombre).toBe('Andrés Vega');
    expect(auth.isAdmin()).toBe(false);
    expect(auth.landingRoute()).toBe('/ordenes-compra');
    auth.refreshUser({ ...user, rol: 'Administrador' });
    expect(auth.landingRoute()).toBe('/dashboard');
    expect(sessionStorage.getItem('intiwasi.session')).toContain(token);
    expect(sessionStorage.getItem('intiwasi.session')).not.toContain('correcta');
  });

  it('valida una sesión restaurada con el backend y descarta una revocada', async () => {
    sessionStorage.setItem(
      'intiwasi.session',
      JSON.stringify({
        token,
        idUsuario: user.idUsuario,
        correo: user.correo,
        nombre: user.nomUsuario,
        rol: user.rol,
        expiresAt: expiry * 1000,
      }),
    );
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);

    const validation = auth.ensure();
    const request = http.expectOne('/api/auth/me');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    request.flush(
      { message: 'Se requiere un token JWT válido' },
      { status: 401, statusText: 'Unauthorized' },
    );
    expect(await validation).toBe(false);
    expect(auth.session()).toBeNull();
    expect(sessionStorage.getItem('intiwasi.session')).toBeNull();
  });

  it('descarta una sesión que ya había vencido al restaurarla', () => {
    sessionStorage.setItem(
      'intiwasi.session',
      JSON.stringify({
        token,
        idUsuario: user.idUsuario,
        correo: user.correo,
        nombre: user.nomUsuario,
        rol: user.rol,
        expiresAt: Date.now() - 1000,
      }),
    );
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);

    expect(auth.session()).toBeNull();
    expect(sessionStorage.getItem('intiwasi.session')).toBeNull();
  });
});
