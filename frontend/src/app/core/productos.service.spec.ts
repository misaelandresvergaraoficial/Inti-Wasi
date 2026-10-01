import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { ProductoRequest, ProductoResponse } from './producto-models';
import { ProductosService } from './productos.service';
import { ProductosList } from '../features/productos/productos-list';

const token = 'jwt-de-prueba';
const product: ProductoResponse = {
  idProducto: 12,
  sku: 'PRO-12',
  nomProducto: 'Procesador',
  idCategoria: 2,
  nomCategoria: 'Procesadores',
  precio: 250,
  stockMinimo: 3,
  stockActual: 0,
  estado: 1,
};
const data: ProductoRequest = {
  sku: product.sku,
  nomProducto: product.nomProducto,
  idCategoria: product.idCategoria,
  idProveedor: null,
  precio: product.precio,
  stockMinimo: product.stockMinimo,
};

describe('Productos conectados al backend', () => {
  let service: ProductosService;
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    sessionStorage.setItem(
      'intiwasi.session',
      JSON.stringify({
        token,
        idUsuario: 1,
        correo: 'admin@intiwasi.test',
        nombre: 'Administrador',
        rol: 'Administrador',
        expiresAt: Date.now() + 3600000,
      }),
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MatDialog, useValue: {} },
      ],
    });
    service = TestBed.inject(ProductosService);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('consulta solo productos activos para el Operador y filtra categorías sin una petición adicional', async () => {
    auth.session.update((current) => ({ ...current!, rol: 'Operador de Almacén' }));
    const list = TestBed.runInInjectionContext(() => new ProductosList());
    const request = http.expectOne('/api/productos');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    http.expectNone('/api/categorias');
    request.flush([product]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(list.error()).toBe('');
    expect(list.records()).toEqual([product]);
    expect(list.categorias()).toEqual(['Procesadores']);
  });

  it('consulta activos e inactivos para el Administrador', async () => {
    const list = TestBed.runInInjectionContext(() => new ProductosList());
    const request = http.expectOne('/api/productos/todos');
    http.expectNone('/api/categorias');
    request.flush([product, { ...product, idProducto: 13, estado: 0 }]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(list.records().map((record) => record.estado)).toEqual([1, 0]);
  });

  it('crea y edita con los campos del contrato del backend', async () => {
    const create = service.save(data);
    const post = http.expectOne('/api/productos');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(data);
    post.flush(product);
    expect((await create).idProducto).toBe(12);

    const update = service.save({ ...data, precio: 280 }, 12);
    const put = http.expectOne('/api/productos/12');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.precio).toBe(280);
    put.flush({ ...product, precio: 280 });
    expect((await update).precio).toBe(280);
  });

  it('desactiva por DELETE y reactiva por PUT con estado 1', async () => {
    const deactivate = service.delete(12);
    const remove = http.expectOne('/api/productos/12');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    await deactivate;

    const activate = service.activate(12);
    const restore = http.expectOne('/api/productos/12/estado');
    expect(restore.request.method).toBe('PUT');
    expect(restore.request.body).toEqual({ estado: 1 });
    restore.flush({ ...product, estado: 1 });
    await activate;
  });

  it('informa el error de stock bajo y permite reintentar la consulta', async () => {
    const first = service.getLowStock();
    http
      .expectOne('/api/productos/stock-bajo')
      .flush({ message: 'No disponible' }, { status: 503, statusText: 'Service Unavailable' });
    await expect(first).rejects.toThrow('El servidor no pudo completar la operación');

    const retry = service.getLowStock();
    http.expectOne('/api/productos/stock-bajo').flush([product]);
    expect(await retry).toEqual([product]);
  });
});
