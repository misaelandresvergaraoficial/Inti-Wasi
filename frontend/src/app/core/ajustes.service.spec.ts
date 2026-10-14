import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AjusteRequest, AjusteResponse } from './ajuste-models';
import { AjustesService } from './ajustes.service';
import { authInterceptor } from './auth.interceptor';

const ajuste: AjusteResponse = {
  idAjuste: 3,
  idDocumento: 18,
  estadoDocumento: 1,
  idDocumentoOrigen: null,
  tipoAjuste: 'Incremento',
  motivo: 'Diferencia física',
  observaciones: null,
  fechaEmision: '2026-10-13T11:00:00',
  idUsuario: 1,
  usuarioResponsable: 'Administradora',
  movimientos: [
    {
      idMovimiento: 1,
      idProducto: 7,
      sku: 'CPU-7',
      nomProducto: 'Procesador',
      cantidad: 2,
      stockActual: 10,
    },
  ],
};
const data: AjusteRequest = {
  tipoAjuste: 'Incremento',
  motivo: 'Diferencia física',
  observaciones: null,
  movimientos: [{ idProducto: 7, cantidad: 2 }],
};

describe('Contrato HTTP de Ajustes', () => {
  let service: AjustesService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem(
      'intiwasi.session',
      JSON.stringify({
        token: 'jwt-admin',
        idUsuario: 1,
        correo: 'admin@intiwasi.test',
        nombre: 'Administradora',
        rol: 'Administrador',
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
    service = TestBed.inject(AjustesService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('consulta listado y detalle con la sesión del administrador', async () => {
    const list = service.list();
    const listing = http.expectOne('/api/ajustes');
    expect(listing.request.headers.get('Authorization')).toBe('Bearer jwt-admin');
    listing.flush([ajuste]);
    expect((await list)[0].idAjuste).toBe(3);
    const detail = service.get(3);
    http.expectOne('/api/ajustes/3').flush(ajuste);
    expect((await detail).movimientos[0].cantidad).toBe(2);
  });

  it('registra diferencias y rectifica con un motivo separado', async () => {
    const created = service.save(data);
    const post = http.expectOne('/api/ajustes');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(data);
    expect(post.request.body.idUsuario).toBeUndefined();
    post.flush(ajuste);
    await created;
    const corrected = service.save(
      { ...data, tipoAjuste: 'Decremento', motivoCorreccion: 'Conteo corregido' },
      3,
    );
    const put = http.expectOne('/api/ajustes/3');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.motivo).toBe('Diferencia física');
    expect(put.request.body.motivoCorreccion).toBe('Conteo corregido');
    put.flush({ ...ajuste, idAjuste: 4, idDocumentoOrigen: 18 });
    expect((await corrected).idAjuste).toBe(4);
  });

  it('envía motivo en DELETE y conserva el rechazo por stock', async () => {
    const annul = service.annul(3, 'Conteo duplicado');
    const deletion = http.expectOne('/api/ajustes/3');
    expect(deletion.request.method).toBe('DELETE');
    expect(deletion.request.body).toEqual({ motivo: 'Conteo duplicado' });
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
