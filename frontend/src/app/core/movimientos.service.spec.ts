import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { MovimientoConsultaResponse, PaginaMovimientos } from './movimiento-models';
import { MovimientosService } from './movimientos.service';

export const movement: MovimientoConsultaResponse = {
  idMovimiento: 17,
  idDocumento: 8,
  idDocumentoOrigen: 4,
  estadoDocumento: 1,
  tipoDocumento: 'Correccion',
  fechaEmision: '2026-10-13T14:00:00',
  idUsuario: 1,
  usuarioResponsable: 'Administradora',
  idProducto: 3,
  sku: 'CPU-3',
  nomProducto: 'Procesador',
  nomCategoria: 'Componentes',
  cantidad: 5,
  cantidadConSigno: -5,
  motivo: 'Recepción duplicada',
  idOrden: 2,
  numeroGuiaRemision: 'G-2',
};
const page: PaginaMovimientos = {
  content: [movement],
  totalElements: 21,
  totalPages: 2,
  number: 1,
};

describe('Consulta HTTP de Movimientos', () => {
  let service: MovimientosService;
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
    service = TestBed.inject(MovimientosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('envía los filtros y la página con orden estable', async () => {
    const listing = service.list(
      {
        fechaInicial: '2026-10-01',
        fechaFinal: '2026-10-13',
        idProducto: 3,
        tipoDocumento: 'Correccion',
        idUsuario: 1,
      },
      1,
    );
    const request = http.expectOne((item) => item.url === '/api/movimientos');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-admin');
    expect(request.request.params.get('fechaFinal')).toBe('2026-10-13');
    expect(request.request.params.get('idProducto')).toBe('3');
    expect(request.request.params.get('tipoDocumento')).toBe('Correccion');
    expect(request.request.params.get('idUsuario')).toBe('1');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('sort')).toBe('idMovimiento,desc');
    request.flush(page);
    expect((await listing).content[0].cantidadConSigno).toBe(-5);
  });

  it('obtiene un detalle y conserva el 404 del backend', async () => {
    const detail = service.get(17);
    http.expectOne('/api/movimientos/17').flush(movement);
    expect((await detail).idDocumentoOrigen).toBe(4);

    const missing = service.get(99);
    http
      .expectOne('/api/movimientos/99')
      .flush(
        { message: 'Movimiento no encontrado con ID: 99' },
        { status: 404, statusText: 'Not Found' },
      );
    await expect(missing).rejects.toMatchObject({ status: 404 });
  });

  it('conserva el rechazo por permisos', async () => {
    const listing = service.list({}, 0);
    http
      .expectOne((item) => item.url === '/api/movimientos')
      .flush(
        { message: 'No tiene permisos suficientes para realizar esta operación' },
        { status: 403, statusText: 'Forbidden' },
      );
    await expect(listing).rejects.toMatchObject({ status: 403 });
  });
});
