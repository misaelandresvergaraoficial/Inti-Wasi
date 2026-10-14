import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { SalidaRequest, SalidaResponse } from './salida-models';
import { SalidasService } from './salidas.service';

const salida: SalidaResponse = {
  idSalida: 5,
  idDocumento: 22,
  estadoDocumento: 1,
  idDocumentoOrigen: null,
  motivo: 'Despacho/Venta',
  observaciones: null,
  fechaEmision: '2026-10-13T11:00:00',
  idUsuario: 2,
  usuarioResponsable: 'Operador',
  movimientos: [
    {
      idMovimiento: 1,
      idProducto: 7,
      sku: 'CPU-7',
      nomProducto: 'Procesador',
      cantidad: 2,
      stockActual: 3,
    },
  ],
};
const data: SalidaRequest = {
  motivo: 'Despacho/Venta',
  observaciones: null,
  movimientos: [{ idProducto: 7, cantidad: 2 }],
};

describe('Contrato HTTP de Salidas', () => {
  let service: SalidasService;
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
    service = TestBed.inject(SalidasService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('consulta listado y detalle con JWT', async () => {
    const list = service.list();
    const listing = http.expectOne('/api/salidas');
    expect(listing.request.headers.get('Authorization')).toBe('Bearer jwt-prueba');
    listing.flush([salida]);
    expect((await list)[0].idSalida).toBe(5);
    const detail = service.get(5);
    http.expectOne('/api/salidas/5').flush(salida);
    expect((await detail).movimientos[0].cantidad).toBe(2);
  });

  it('registra y rectifica sin asignar responsable ni estado desde el cliente', async () => {
    const created = service.save(data);
    const post = http.expectOne('/api/salidas');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(data);
    expect(post.request.body.idUsuario).toBeUndefined();
    post.flush(salida);
    await created;
    const corrected = service.save({ ...data, motivoCorreccion: 'Cantidad errónea' }, 5);
    const put = http.expectOne('/api/salidas/5');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.motivoCorreccion).toBe('Cantidad errónea');
    put.flush({ ...salida, idSalida: 6, idDocumentoOrigen: 22 });
    expect((await corrected).idSalida).toBe(6);
  });

  it('envía motivo en DELETE y expone el rechazo por stock insuficiente', async () => {
    const annul = service.annul(5, 'Salida duplicada');
    const deletion = http.expectOne('/api/salidas/5');
    expect(deletion.request.method).toBe('DELETE');
    expect(deletion.request.body).toEqual({ motivo: 'Salida duplicada' });
    deletion.flush(
      { message: 'Stock insuficiente para el producto: 7' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    await expect(annul).rejects.toMatchObject({
      status: 422,
      message: 'Stock insuficiente para el producto: 7',
    });
  });
});
