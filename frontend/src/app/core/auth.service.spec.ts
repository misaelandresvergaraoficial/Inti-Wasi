import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { UsuarioResponse } from './models';

const user: UsuarioResponse = {
  idUsuario: 6, nomUsuario: 'Andrés Vega', correo: 'andres@intiwasi.test',
  rol: 'Operador de Almacén', telefono: null, estado: 1, fechaRegistro: '2026-09-01T09:00:00',
};

describe('Sesión mediante cookie', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  const response = () => ({ usuario: user, expiresAt: Date.now() + 8 * 60 * 60_000 });

  beforeEach(() => {
    vi.useFakeTimers();
    sessionStorage.clear();
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([]),
    ] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    for (const request of http.match(() => true)) if (!request.cancelled) request.flush(null);
    http.verify();
    TestBed.resetTestingModule();
    vi.useRealTimers();
    sessionStorage.clear();
    localStorage.clear();
  });

  async function openSession() {
    const login = auth.login(user.correo, 'correcta');
    http.expectOne('/api/auth/csrf').flush({});
    await Promise.resolve();
    const request = http.expectOne('/api/auth/login');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.body).toEqual({ correo: user.correo, contrasena: 'correcta' });
    request.flush(response());
    await Promise.resolve();
    http.expectOne('/api/auth/csrf').flush({});
    await login;
  }

  it('obtiene CSRF antes y después del login y no guarda la sesión en Web Storage', async () => {
    await openSession();
    expect(auth.session()?.nombre).toBe('Andrés Vega');
    expect(auth.landingRoute()).toBe('/ordenes-compra');
    expect(sessionStorage.getItem('intiwasi.session')).toBeNull();
    expect(localStorage.getItem('intiwasi.session')).toBeNull();
    auth.refreshUser({ ...user, rol: 'Administrador' });
    expect(auth.landingRoute()).toBe('/dashboard');
  });

  it('envía el encabezado CSRF desde la cookie en una mutación relativa', () => {
    document.cookie = 'XSRF-TOKEN=csrf-test; path=/';
    TestBed.inject(HttpClient).post('/api/auth/login', {}).subscribe();
    const request = http.expectOne('/api/auth/login');
    expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test');
    request.flush({});
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/';
  });

  it('conserva el mensaje de cuenta inactiva al rechazar el login', async () => {
    const login = auth.login(user.correo, 'incorrecta');
    http.expectOne('/api/auth/csrf').flush({});
    await Promise.resolve();
    http.expectOne('/api/auth/login').flush(
      { code: 'ACCOUNT_INACTIVE' },
      { status: 401, statusText: 'Unauthorized' },
    );
    await expect(login).rejects.toMatchObject({ status: 401, message: 'Su cuenta no está disponible. Contacte al Administrador.' });
    expect(auth.session()).toBeNull();
  });

  it('elimina la sesión legada al iniciar y restaura la identidad con /me', async () => {
    sessionStorage.setItem('intiwasi.session', 'legado');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    expect(sessionStorage.getItem('intiwasi.session')).toBeNull();
    const validation = auth.ensure();
    http.expectOne('/api/auth/me').flush(response());
    expect(await validation).toBe(true);
    expect(auth.session()?.idUsuario).toBe(user.idUsuario);
  });

  it('cierra en el servidor y espera su respuesta antes de otro login', async () => {
    await openSession();
    auth.logout();
    http.expectOne('/api/auth/csrf').flush({});
    await Promise.resolve();
    const logout = http.expectOne('/api/auth/logout');
    expect(logout.request.headers.has('Authorization')).toBe(false);
    expect(auth.session()).toBeNull();
    const login = auth.login(user.correo, 'correcta');
    http.expectNone('/api/auth/csrf');
    logout.flush(null);
    for (let i = 0; i < 8; i++) await Promise.resolve();
    http.expectOne('/api/auth/csrf').flush({});
    await Promise.resolve();
    http.expectOne('/api/auth/login').flush(response());
    await Promise.resolve();
    http.expectOne('/api/auth/csrf').flush({});
    await login;
  });

  it('adquiere Web Lock antes de iniciar y refresca CSRF dentro del bloqueo', async () => {
    let release!: () => void;
    const requestLock = vi.fn((_name: string, callback: () => Promise<void>) =>
      new Promise<void>((resolve, reject) => { release = () => { void callback().then(resolve, reject); }; }),
    );
    Object.defineProperty(navigator, 'locks', { configurable: true, value: { request: requestLock } });
    try {
      const login = auth.login(user.correo, 'correcta');
      expect(requestLock).toHaveBeenCalledWith('intiwasi.auth', expect.any(Function));
      http.expectNone('/api/auth/csrf');
      release();
      http.expectOne('/api/auth/csrf').flush({});
      await Promise.resolve();
      http.expectOne('/api/auth/login').flush(response());
      await Promise.resolve();
      http.expectOne('/api/auth/csrf').flush({});
      await login;
    } finally {
      Reflect.deleteProperty(navigator, 'locks');
    }
  });

  it('refresca CSRF dentro del bloqueo antes de cerrar', async () => {
    await openSession();
    let release!: () => void;
    const requestLock = vi.fn((_name: string, callback: () => Promise<void>) =>
      new Promise<void>((resolve, reject) => { release = () => { void callback().then(resolve, reject); }; }),
    );
    Object.defineProperty(navigator, 'locks', { configurable: true, value: { request: requestLock } });
    try {
      auth.logout();
      expect(requestLock).toHaveBeenCalledWith('intiwasi.auth', expect.any(Function));
      http.expectNone('/api/auth/csrf');
      release();
      http.expectOne('/api/auth/csrf').flush({});
      await Promise.resolve();
      http.expectOne('/api/auth/logout').flush(null);
      for (let i = 0; i < 8; i++) await Promise.resolve();
    } finally {
      Reflect.deleteProperty(navigator, 'locks');
    }
  });

  it('no revoca una sesión reemplazada mientras el cierre esperaba el bloqueo', async () => {
    await openSession();
    let release!: () => void;
    Object.defineProperty(navigator, 'locks', { configurable: true, value: {
      request: (_name: string, callback: () => Promise<void>) =>
        new Promise<void>((resolve, reject) => { release = () => { void callback().then(resolve, reject); }; }),
    } });
    try {
      auth.logout();
      http.expectNone('/api/auth/csrf');
      localStorage.setItem('intiwasi.authChange', 'revision-nueva');
      release();
      for (let i = 0; i < 8; i++) await Promise.resolve();
      http.expectNone('/api/auth/logout');
    } finally {
      Reflect.deleteProperty(navigator, 'locks');
    }
  });

  it('un 401 revalida la cookie y redirige si la sesión ya no existe', async () => {
    await openSession();
    TestBed.inject(HttpClient).get('/api/productos').subscribe({ error: () => {} });
    const request = http.expectOne('/api/productos');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve(); await Promise.resolve(); await Promise.resolve();
    expect(auth.session()).toBeNull();
  });

  it('un 401 tardío de una solicitud anterior no borra un nuevo login', async () => {
    await openSession();
    TestBed.inject(HttpClient).get('/api/productos').subscribe({ error: () => {} });
    const old = http.expectOne('/api/productos');
    await openSession();
    const current = auth.session();
    old.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.session()).toBe(current);
    http.expectNone('/api/auth/me');
  });

  it('una comprobación tardía no borra una sesión nueva', async () => {
    const validation = auth.ensure();
    const old = http.expectOne('/api/auth/me');
    await openSession();
    const current = auth.session();
    old.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(await validation).toBe(false);
    expect(auth.session()).toBe(current);
  });

  it('la revalidación por evento entre pestañas no inicia un cierre mutable', async () => {
    await openSession();
    vi.setSystemTime(Date.now() + 16 * 60_000);
    localStorage.setItem('intiwasi.authChange', 'revision-nueva');
    window.dispatchEvent(new StorageEvent('storage', { key: 'intiwasi.authChange' }));
    http.expectOne('/api/auth/me').flush(response());
    for (let i = 0; i < 5; i++) await Promise.resolve();
    http.expectNone('/api/auth/logout');
    http.expectNone('/api/auth/csrf');
  });

  it('comparte la actividad entre pestañas y cierra tras el plazo', async () => {
    await openSession();
    vi.advanceTimersByTime(15 * 60_000);
    expect(auth.idleSecondsRemaining()).toBe(30);
    localStorage.setItem('intiwasi.lastActivity', String(Date.now()));
    vi.advanceTimersByTime(1000);
    expect(auth.idleSecondsRemaining()).toBeNull();
    vi.advanceTimersByTime(15 * 60_000 + 30_000);
    expect(auth.session()).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login'], { queryParams: { sesion: 'inactiva' } });
  });

  it('solo envía actividad ante interacción humana y conserva la expiración absoluta', async () => {
    await openSession();
    const expiry = auth.session()!.expiresAt;
    vi.advanceTimersByTime(60_000);
    http.expectNone('/api/auth/actividad');
    document.dispatchEvent(new Event('keydown'));
    http.expectOne('/api/auth/actividad').flush(null);
    expect(auth.session()!.expiresAt).toBe(expiry);
  });
});
