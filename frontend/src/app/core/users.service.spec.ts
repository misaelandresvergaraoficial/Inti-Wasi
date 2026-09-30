import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { UsersService } from './users.service';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { UsuarioRequest, UsuarioResponse } from './models';

const token = 'jwt-de-prueba';
const user: UsuarioResponse = {
  idUsuario: 6,
  nomUsuario: 'Andrés Vega',
  correo: 'andres@intiwasi.test',
  rol: 'Operador de Almacén',
  telefono: null,
  estado: 1,
  fechaRegistro: '2026-09-01T09:00:00',
};
const edit: UsuarioRequest = {
  nomUsuario: 'Andrés Vega',
  correo: 'andres@intiwasi.test',
  rol: 'Operador de Almacén',
  telefono: null,
  contrasena: undefined,
};

describe('Usuarios conectados al backend', () => {
  let users: UsersService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem(
      'intiwasi.session',
      JSON.stringify({
        token,
        idUsuario: user.idUsuario,
        correo: user.correo,
        nombre: user.nomUsuario,
        rol: user.rol,
        expiresAt: Date.now() + 3600000,
      }),
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    users = TestBed.inject(UsersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('consulta todas las cuentas, incluidas las inactivas, con JWT', async () => {
    const result = users.list();
    const request = http.expectOne('/api/usuarios/todos');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    request.flush([user, { ...user, idUsuario: 7, estado: 0 }]);
    expect((await result).map((record) => record.estado)).toEqual([1, 0]);
  });

  it('no envía el JWT al login ni a servicios ajenos a la API', () => {
    const client = TestBed.inject(HttpClient);
    client.post('/api/auth/login', {}).subscribe();
    const login = http.expectOne('/api/auth/login');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({});

    client.get('https://example.com/api/datos').subscribe();
    const external = http.expectOne('https://example.com/api/datos');
    expect(external.request.headers.has('Authorization')).toBe(false);
    external.flush({});
  });

  it('desactiva con DELETE y reactiva la misma cuenta con PUT de estado', async () => {
    const deactivate = users.deactivate(6);
    const deletion = http.expectOne('/api/usuarios/6');
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    await deactivate;

    const activate = users.activate(6);
    const restoration = http.expectOne('/api/usuarios/6/estado');
    expect(restoration.request.method).toBe('PUT');
    expect(restoration.request.body).toEqual({ estado: 1 });
    restoration.flush(user);
    await activate;
  });

  it('edita datos personales sin enviar un cambio de estado', async () => {
    const result = users.save(edit, 6);
    const request = http.expectOne('/api/usuarios/6');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.estado).toBeUndefined();
    request.flush({ ...user, estado: 0 });
    expect((await result).estado).toBe(0);
  });

  it('asocia un conflicto del backend al campo correo', async () => {
    const result = users.save(edit);
    const request = http.expectOne('/api/usuarios');
    request.flush(
      { message: 'El correo ya se encuentra registrado' },
      { status: 409, statusText: 'Conflict' },
    );
    await expect(result).rejects.toMatchObject({ status: 409, field: 'correo' });
  });

  it('cierra una sesión rechazada por el backend', async () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const result = users.list();
    const request = http.expectOne('/api/usuarios/todos');
    request.flush(
      { message: 'Se requiere un token JWT válido' },
      { status: 401, statusText: 'Unauthorized' },
    );

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(TestBed.inject(AuthService).session()).toBeNull();
    expect(sessionStorage.getItem('intiwasi.session')).toBeNull();
  });
});
