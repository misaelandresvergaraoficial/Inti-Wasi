import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { OrdenRequest, OrdenResponse, puedeCancelarOrden, puedeEditarOrden } from './order-models';
import { OrdersService } from './orders.service';

const token = 'jwt-de-prueba';
const order: OrdenResponse = {
  idOrden: 12,
  idProveedor: 3,
  nomProveedor: 'Proveedor Norte',
  idUsuario: 1,
  nomUsuario: 'Valeria Torres',
  fechaEmision: '2026-09-29',
  fechaEstimadaEntrega: '2026-10-03',
  estado: 'Pendiente',
  detalles: [
    {
      idDetalle: 1,
      idProducto: 4,
      sku: 'CPU-04',
      nomProducto: 'Procesador',
      cantidad: 5,
      cantidadRecibida: 0,
      cantidadPorRecibir: 5,
      precioUnitario: 200,
      subtotal: 1000,
    },
  ],
  totalOrden: 1000,
};
const data: OrdenRequest = {
  idProveedor: 3,
  fechaEstimadaEntrega: '2026-10-03',
  detalles: [{ idProducto: 4, cantidad: 5, precioUnitario: 200 }],
};

describe('Órdenes de compra conectadas al backend', () => {
  let orders: OrdersService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem(
      'intiwasi.session',
      JSON.stringify({
        token,
        idUsuario: 1,
        correo: 'admin@intiwasi.test',
        nombre: 'Valeria Torres',
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
    orders = TestBed.inject(OrdersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('consulta listado y detalle con JWT', async () => {
    const list = orders.list();
    const listing = http.expectOne('/api/ordenes-compra');
    expect(listing.request.method).toBe('GET');
    expect(listing.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    listing.flush([order]);
    expect((await list)[0].idOrden).toBe(12);

    const detail = orders.get(12);
    const request = http.expectOne('/api/ordenes-compra/12');
    expect(request.request.method).toBe('GET');
    request.flush(order);
    expect((await detail).detalles[0].cantidadPorRecibir).toBe(5);
  });

  it('usa los catálogos activos solo para elegir proveedor y producto', async () => {
    const suppliers = orders.suppliers();
    const supplierRequest = http.expectOne('/api/proveedores');
    expect(supplierRequest.request.method).toBe('GET');
    supplierRequest.flush([{ idProveedor: 3, nomProveedor: 'Proveedor Norte', estado: 1 }]);
    expect((await suppliers)[0].idProveedor).toBe(3);

    const products = orders.products();
    const productRequest = http.expectOne('/api/productos');
    expect(productRequest.request.method).toBe('GET');
    productRequest.flush([{ idProducto: 4, sku: 'CPU-04', nomProducto: 'Procesador', estado: 1 }]);
    expect((await products)[0].idProducto).toBe(4);
  });

  it('crea, edita y cancela sin enviar un estado manual', async () => {
    const create = orders.save(data);
    const post = http.expectOne('/api/ordenes-compra');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(data);
    expect(post.request.body.estado).toBeUndefined();
    post.flush(order);
    await create;

    const update = orders.save(data, 12);
    const put = http.expectOne('/api/ordenes-compra/12');
    expect(put.request.method).toBe('PUT');
    put.flush(order);
    await update;

    const cancel = orders.cancel(12);
    const deletion = http.expectOne('/api/ordenes-compra/12');
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    await cancel;
  });

  it('respeta los estados y conserva el rechazo de una edición con entradas', async () => {
    expect(puedeEditarOrden(order)).toBe(true);
    expect(puedeCancelarOrden(order)).toBe(true);
    expect(puedeEditarOrden({ ...order, estado: 'Parcial' })).toBe(false);
    expect(puedeCancelarOrden({ ...order, estado: 'Parcial' })).toBe(true);
    expect(puedeCancelarOrden({ ...order, estado: 'Recibida' })).toBe(false);

    const update = orders.save(data, 12);
    http
      .expectOne('/api/ordenes-compra/12')
      .flush(
        { message: 'Solo se puede editar una orden pendiente sin entradas registradas' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await expect(update).rejects.toMatchObject({
      status: 422,
      message: 'Solo se puede editar una orden pendiente sin entradas registradas',
    });
  });

  it('cierra una sesión rechazada por el servidor', async () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const result = orders.list();
    http
      .expectOne('/api/ordenes-compra')
      .flush(
        { message: 'Se requiere un token JWT válido' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(TestBed.inject(AuthService).session()).toBeNull();
  });
});
