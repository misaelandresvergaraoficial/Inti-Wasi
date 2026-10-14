import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { EntradaRequest, EntradaResponse } from './entrada-models';
import { EntradasService } from './entradas.service';

const entry: EntradaResponse = {
  idEntrada: 8,
  idDocumento: 31,
  idOrden: 4,
  numeroGuiaRemision: 'G-2026-08',
  estadoDocumento: 1,
  idDocumentoOrigen: null,
  observaciones: null,
  fechaEmision: '2026-10-13T11:00:00',
  idUsuario: 2,
  usuarioResponsable: 'Operador',
  estadoOrden: 'Parcial',
  movimientos: [
    {
      idMovimiento: 1,
      idProducto: 7,
      sku: 'CPU-7',
      nomProducto: 'Procesador',
      cantidad: 2,
      stockActual: 6,
    },
  ],
};
const data: EntradaRequest = {
  idOrden: 4,
  numeroGuiaRemision: 'G-2026-08',
  observaciones: null,
  movimientos: [{ idProducto: 7, cantidad: 2 }],
};

describe('Contrato HTTP de Entradas', () => {
  let service: EntradasService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem(
      'intiwasi.session',
      JSON.stringify({
        token: 'jwt-prueba',
        idUsuario: 2,
        correo: 'operador@intiwasi.test',
        nombre: 'Operador',
        rol: 'Operador de Almacén',
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
    service = TestBed.inject(EntradasService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('consulta listado y detalle con la sesión del operador', async () => {
    const list = service.list();
    const listing = http.expectOne('/api/entradas');
    expect(listing.request.method).toBe('GET');
    expect(listing.request.headers.get('Authorization')).toBe('Bearer jwt-prueba');
    listing.flush([entry]);
    expect((await list)[0].estadoDocumento).toBe(1);
    const detail = service.get(8);
    http.expectOne('/api/entradas/8').flush(entry);
    expect((await detail).movimientos[0].cantidad).toBe(2);
  });

  it('registra y rectifica con motivo separado, sin asignar usuario ni estado desde el cliente', async () => {
    const created = service.save(data);
    const post = http.expectOne('/api/entradas');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(data);
    expect(post.request.body.idUsuario).toBeUndefined();
    expect(post.request.body.estadoDocumento).toBeUndefined();
    post.flush(entry);
    await created;
    const corrected = service.save({ ...data, motivoCorreccion: 'Guía errónea' }, 8);
    const put = http.expectOne('/api/entradas/8');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.motivoCorreccion).toBe('Guía errónea');
    put.flush({ ...entry, idEntrada: 9, idDocumentoOrigen: 31 });
    expect((await corrected).idEntrada).toBe(9);
  });

  it('envía el motivo en el cuerpo de DELETE y conserva el error de negocio', async () => {
    const annul = service.annul(8, 'Recepción duplicada');
    const deletion = http.expectOne('/api/entradas/8');
    expect(deletion.request.method).toBe('DELETE');
    expect(deletion.request.body).toEqual({ motivo: 'Recepción duplicada' });
    deletion.flush(
      { message: 'La corrección dejaría stock negativo' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    await expect(annul).rejects.toMatchObject({
      status: 422,
      message: 'La corrección dejaría stock negativo',
    });
  });
});
