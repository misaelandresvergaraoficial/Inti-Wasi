import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { OrdersService } from '../../core/orders.service';
import { OrdenResponse } from '../../core/order-models';
import { Pagina } from '../../core/pagination';
import { OrdersList } from './orders-list';

const order: OrdenResponse = {
  idOrden: 7, idProveedor: 2, nomProveedor: 'Proveedor', prefijoGuia: 'GUIA-PRUEBA-',
  idUsuario: 1, nomUsuario: 'Admin', fechaEmision: '2026-11-02', fechaEstimadaEntrega: null,
  estado: 'Parcial', editable: false, totalOrden: 100,
  detalles: [{ idDetalle: 1, idProducto: 3, sku: 'SKU-3', nomProducto: 'Producto', cantidad: 10,
    cantidadRecibida: 4, cantidadPorRecibir: 6, precioUnitario: 10, subtotal: 100 }],
};
const pagina: Pagina<OrdenResponse> = { content: [order], totalElements: 9, totalPages: 2, number: 0 };

function diferida<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolver) => { resolve = resolver; });
  return { promise, resolve };
}

describe('OrdersList consultas y paginación', () => {
  async function setup() {
    const orders = {
      list: vi.fn<OrdersService['list']>().mockResolvedValue(pagina),
      cancel: vi.fn().mockResolvedValue(undefined),
    };
    const auth = { isAdmin: vi.fn().mockReturnValue(true) };
    const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
    const notice = { clear: vi.fn(), show: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: OrdersService, useValue: orders },
        { provide: AuthService, useValue: auth },
        { provide: MatDialog, useValue: dialog },
        { provide: NoticeService, useValue: notice },
      ],
    });
    const component = TestBed.runInInjectionContext(() => new OrdersList());
    await component.load();
    orders.list.mockClear();
    return { component, orders, auth, dialog, notice };
  }

  it('ignora páginas negativas, fraccionarias, fuera del final y la página actual', async () => {
    const { component, orders } = await setup();
    for (const page of [-1, 0, 0.5, 2, Number.NaN]) component.changePage(page);
    expect(component.page()).toBe(0);
    expect(orders.list).not.toHaveBeenCalled();
  });

  it('consulta la última página válida y puede volver a la primera', async () => {
    const { component, orders } = await setup();
    component.changePage(1);
    expect(component.page()).toBe(1);
    expect(orders.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 8 }));
    component.changePage(2);
    expect(orders.list).toHaveBeenCalledTimes(1);
    component.changePage(0);
    expect(component.page()).toBe(0);
    expect(orders.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0, size: 8 }));
    expect(orders.list).toHaveBeenCalledTimes(2);
  });

  it('conserva la búsqueda más reciente cuando una respuesta anterior llega tarde', async () => {
    const { component, orders } = await setup();
    const anterior = diferida<Pagina<OrdenResponse>>();
    orders.list.mockReturnValueOnce(anterior.promise).mockResolvedValueOnce({ ...pagina, content: [{ ...order, idOrden: 8 }] });
    const consultaAnterior = component.load();
    component.search('proveedor reciente');
    await Promise.resolve();
    anterior.resolve(pagina);
    await consultaAnterior;
    expect(component.records()[0].idOrden).toBe(8);
    expect(component.query()).toBe('proveedor reciente');
    expect(component.loading()).toBe(false);
  });

  it('informa un fallo de consulta y permite recuperarse sin conservar el error', async () => {
    const { component, orders } = await setup();
    orders.list.mockRejectedValueOnce(new Error('Conexión interrumpida'));
    await component.load();
    expect(component.error()).toBe('Conexión interrumpida');
    expect(component.loading()).toBe(false);
    await component.load();
    expect(component.error()).toBe('');
    expect(component.records()).toEqual([order]);
    expect(component.total()).toBe(9);
    expect(component.rangeEnd()).toBe(8);
  });

  it('bloquea fechas invertidas sin petición y reinicia página al corregir filtros', async () => {
    const { component, orders } = await setup();
    component.desde.set('2026-11-03');
    component.hasta.set('2026-11-02');
    await component.load();
    expect(component.dateError()).toContain('posterior');
    expect(orders.list).not.toHaveBeenCalled();
    expect(component.records()).toEqual([]);
    component.page.set(1);
    component.changeDate('hasta', '2026-11-04');
    expect(component.page()).toBe(0);
    expect(orders.list).toHaveBeenLastCalledWith(expect.objectContaining({ desde: '2026-11-03', hasta: '2026-11-04', page: 0 }));
    component.page.set(1);
    component.filterStatus('Parcial');
    expect(component.page()).toBe(0);
    expect(orders.list).toHaveBeenLastCalledWith(expect.objectContaining({ estado: 'Parcial', page: 0 }));
    component.page.set(1);
    component.changeSort('asc');
    expect(component.page()).toBe(0);
    expect(orders.list).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'asc' }));
    expect(component.hasFilters()).toBe(true);
    component.reset();
    expect(component.hasFilters()).toBe(false);
    expect(orders.list).toHaveBeenLastCalledWith({ page: 0, size: 8, q: '', estado: '', desde: '', hasta: '', sort: 'desc' });
  });

  it('respeta los estados de compra y suma cantidades recibidas y solicitadas', async () => {
    const { component } = await setup();
    const multiple = { ...order, detalles: [...order.detalles, { ...order.detalles[0], idDetalle: 2, cantidad: 5, cantidadRecibida: 1 }] };
    expect(component.receivedUnits(multiple)).toBe(5);
    expect(component.requestedUnits(multiple)).toBe(15);
    expect(component.canEdit(order)).toBe(false);
    expect(component.canEdit({ ...order, estado: 'Pendiente', editable: true })).toBe(true);
    expect(component.canCancel(order)).toBe(true);
    expect(component.canCancel({ ...order, estado: 'Recibida' })).toBe(false);
    expect(component.visible()).toEqual([order]);
    expect(component.filtered()).toEqual([order]);
  });

  it('no abre confirmación para operador, orden cerrada ni durante otra cancelación', async () => {
    const { component, orders, auth, dialog } = await setup();
    auth.isAdmin.mockReturnValue(false);
    await component.cancel(order);
    auth.isAdmin.mockReturnValue(true);
    await component.cancel({ ...order, estado: 'Recibida' });
    component.busyId.set(99);
    await component.cancel(order);
    expect(dialog.open).not.toHaveBeenCalled();
    expect(orders.cancel).not.toHaveBeenCalled();
  });

  it('cancelar el diálogo no modifica la compra ni solicita su cancelación', async () => {
    const { component, orders, dialog } = await setup();
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    await component.cancel(order);
    expect(orders.cancel).not.toHaveBeenCalled();
    expect(component.busyId()).toBeNull();
  });

  it('informa conservación de recepciones, bloquea envío duplicado y recarga tras cancelar', async () => {
    const { component, orders, dialog, notice } = await setup();
    const cancelacion = diferida<void>();
    orders.cancel.mockReturnValueOnce(cancelacion.promise);
    const pendiente = component.cancel(order);
    await Promise.resolve();
    expect(component.busyId()).toBe(order.idOrden);
    await component.cancel(order);
    expect(orders.cancel).toHaveBeenCalledTimes(1);
    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ data: expect.objectContaining({ message: expect.stringContaining('historial se conservarán') }) }));
    cancelacion.resolve();
    await pendiente;
    expect(orders.list).toHaveBeenCalledTimes(1);
    expect(component.busyId()).toBeNull();
    expect(notice.clear).toHaveBeenCalled();
    expect(notice.show).toHaveBeenCalledWith('La orden N.° 7 quedó Cancelada.');
  });

  it('un fallo al cancelar libera el bloqueo y deja un mensaje para reintentar', async () => {
    const { component, orders, notice } = await setup();
    orders.cancel.mockRejectedValueOnce(new Error('No se pudo conectar'));
    await component.cancel({ ...order, detalles: [{ ...order.detalles[0], cantidadRecibida: 0 }] });
    expect(component.error()).toBe('No se pudo conectar');
    expect(component.busyId()).toBeNull();
    expect(notice.show).not.toHaveBeenCalled();
  });
});
