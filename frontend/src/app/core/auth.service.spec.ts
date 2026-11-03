import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
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
    vi.useFakeTimers();
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
    for (const request of http.match(
      (request) =>
        request.method === 'POST' &&
        ['/api/auth/logout', '/api/auth/actividad'].includes(request.url),
    )) {
      if (!request.cancelled) request.flush(null);
    }
    http.verify();
    TestBed.resetTestingModule();
    vi.useRealTimers();
    sessionStorage.clear();
  });

  async function openSession() {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const futureToken = `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 28800 }))}.signature`;
    const login = auth.login(user.correo, 'correcta');
    http.expectOne('/api/auth/login').flush({ token: futureToken });
    await Promise.resolve();
    http.expectOne('/api/auth/me').flush(user);
    await login;
    return auth.session()!.expiresAt;
  }

  it('cerrar sesión solicita revocación con el token anterior y borra la sesión local', async () => {
    await openSession();
    const previousToken = auth.token();
    auth.logout();
    const request = http.expectOne('/api/auth/logout');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${previousToken}`);
    expect(auth.session()).toBeNull();
    request.flush(null);
  });

  it('solo comunica actividad humana, sin renovar por consultas automáticas', async () => {
    await openSession();
    vi.advanceTimersByTime(60_000);
    auth.token();
    http.expectNone('/api/auth/actividad');
    document.dispatchEvent(new Event('keydown'));
    const request = http.expectOne('/api/auth/actividad');
    expect(request.request.method).toBe('POST');
    request.flush(null);
  });

  it('una respuesta 401 de la API descarta la sesión revocada', async () => {
    await openSession();
    const client = TestBed.inject(HttpClient);
    client.get('/api/productos').subscribe({ error: () => {} });
    http.expectOne('/api/productos').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.session()).toBeNull();
  });

  it('el rechazo tardío de un token anterior no cierra una sesión nueva', async () => {
    await openSession();
    TestBed.inject(HttpClient)
      .get('/api/productos')
      .subscribe({ error: () => {} });
    const previousRequest = http.expectOne('/api/productos');
    vi.advanceTimersByTime(1000);
    await openSession();
    const currentToken = auth.token();
    previousRequest.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token()).toBe(currentToken);
    http.expectNone('/api/auth/logout');
  });

  it('avisa a los quince minutos y cierra treinta segundos después sin confirmación', async () => {
    await openSession();
    vi.advanceTimersByTime(15 * 60_000 - 1);
    expect(auth.idleSecondsRemaining()).toBeNull();
    vi.advanceTimersByTime(1);
    auth.valid();
    expect(auth.idleSecondsRemaining()).toBe(30);
    document.dispatchEvent(new Event('pointerdown'));
    vi.advanceTimersByTime(30_000);
    auth.valid();
    expect(auth.session()).toBeNull();
    expect(sessionStorage.getItem('intiwasi.session')).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { sesion: 'inactiva' },
    });
  });

  it('confirmar reinicia inactividad sin extender la expiración absoluta', async () => {
    const expiresAt = await openSession();
    vi.advanceTimersByTime(15 * 60_000);
    auth.valid();
    auth.continueSession();
    expect(auth.idleSecondsRemaining()).toBeNull();
    expect(auth.session()!.expiresAt).toBe(expiresAt);
    vi.advanceTimersByTime(14 * 60_000);
    expect(auth.valid()).toBe(true);
    expect(auth.idleSecondsRemaining()).toBeNull();
  });

  it('actividad humana reinicia el tiempo pero consultar el token no lo hace', async () => {
    await openSession();
    vi.advanceTimersByTime(10 * 60_000);
    document.dispatchEvent(new Event('keydown'));
    vi.advanceTimersByTime(10 * 60_000);
    expect(auth.token()).not.toBeNull();
    expect(auth.idleSecondsRemaining()).toBeNull();
    vi.advanceTimersByTime(5 * 60_000);
    auth.token();
    expect(auth.idleSecondsRemaining()).toBe(30);
  });

  it('al volver del segundo plano usa el tiempo real y no acepta confirmaciones tardías', async () => {
    await openSession();
    vi.setSystemTime(Date.now() + 16 * 60_000);
    window.dispatchEvent(new Event('pageshow'));
    auth.continueSession();
    expect(auth.session()).toBeNull();
  });

  it('recargar la sesión conserva el plazo del aviso', async () => {
    await openSession();
    vi.advanceTimersByTime(15 * 60_000 + 10_000);
    expect(auth.idleSecondsRemaining()).toBe(20);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    expect(auth.idleSecondsRemaining()).toBe(20);
    vi.advanceTimersByTime(20_000);
    expect(auth.session()).toBeNull();
  });

  it('rechaza continuar cuando el token ha vencido aunque no haya inactividad', async () => {
    const expiresAt = await openSession();
    vi.setSystemTime(expiresAt);
    auth.continueSession();
    expect(auth.session()).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { sesion: 'caducada' },
    });
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

  it('un fallo tardío al comprobar la identidad no borra un nuevo login', async () => {
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
    const previous = http.expectOne('/api/auth/me');
    await openSession();
    const current = auth.session()!.token;
    previous.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(await validation).toBe(false);
    expect(auth.session()?.token).toBe(current);
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
