import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { ReportsService } from './reports.service';

describe('Reportes y dashboard conectados a la API', () => {
  let service: ReportsService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem('intiwasi.session', JSON.stringify({
      token: 'jwt-prueba', idUsuario: 1, correo: 'admin@intiwasi.pe', nombre: 'Admin',
      rol: 'Administrador', expiresAt: Date.now() + 3600000,
    }));
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([]),
    ] });
    service = TestBed.inject(ReportsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); sessionStorage.clear(); });

  it('consulta el resumen protegido con JWT', async () => {
    const result = service.resumen();
    const request = http.expectOne('/api/dashboard/resumen');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-prueba');
    request.flush({ totalProductosActivos: 5, productosConStockBajo: 2, entradasDelDia: 1,
      salidasDelDia: 0, productosPorReponer: [] });
    expect((await result).productosConStockBajo).toBe(2);
  });

  it('envía filtros y paginación a movimientos', async () => {
    const result = service.consultar('movimientos', {
      fechaInicial: '2026-10-01', fechaFinal: '2026-10-09', tipoMovimiento: 'Entrada', idProducto: 3,
    }, 2);
    const request = http.expectOne((r) => r.url === '/api/reportes/movimientos');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('fechaInicial')).toBe('2026-10-01');
    expect(request.request.params.get('tipoMovimiento')).toBe('Entrada');
    expect(request.request.params.get('idProducto')).toBe('3');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 2 });
    expect((await result).content).toEqual([]);
  });

  it('reconoce una exportación sin resultados como 204', async () => {
    const result = service.exportar('reposicion', 'pdf', {});
    const request = http.expectOne((r) => r.url === '/api/reportes/reposicion/exportar');
    expect(request.request.params.get('formato')).toBe('pdf');
    expect(request.request.responseType).toBe('blob');
    request.flush(null, { status: 204, statusText: 'No Content' });
    expect((await result).status).toBe(204);
  });
});
