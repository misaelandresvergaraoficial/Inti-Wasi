import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { UsersService } from './users.service';
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
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
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
});
