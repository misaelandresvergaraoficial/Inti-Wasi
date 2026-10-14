import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { DocumentoResponse } from './documento-models';
import { DocumentosService } from './documentos.service';

const documento: DocumentoResponse = {
  idDocumento: 7,
  tipoDocumento: 'Correccion',
  fechaEmision: '2026-10-13T12:00:00',
  estado: 1,
  idDocumentoOrigen: 4,
  motivoCorreccion: 'Cantidad equivocada',
  idUsuario: 1,
  usuarioResponsable: 'Administradora',
};

describe('Consulta HTTP de Documentos', () => {
  let service: DocumentosService;
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
    service = TestBed.inject(DocumentosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('consulta listado y detalle sin enviar acciones de escritura', async () => {
    const listing = service.list();
    const getAll = http.expectOne('/api/documentos');
    expect(getAll.request.method).toBe('GET');
    expect(getAll.request.headers.get('Authorization')).toBe('Bearer jwt-admin');
    getAll.flush([documento]);
    expect((await listing)[0].idDocumentoOrigen).toBe(4);

    const detail = service.get(7);
    const getOne = http.expectOne('/api/documentos/7');
    expect(getOne.request.method).toBe('GET');
    getOne.flush(documento);
    expect((await detail).motivoCorreccion).toBe('Cantidad equivocada');
  });

  it('conserva el rechazo de permisos del backend', async () => {
    const listing = service.list();
    http
      .expectOne('/api/documentos')
      .flush(
        { message: 'No tiene permisos suficientes para realizar esta operación' },
        { status: 403, statusText: 'Forbidden' },
      );
    await expect(listing).rejects.toMatchObject({ status: 403 });
  });
});
