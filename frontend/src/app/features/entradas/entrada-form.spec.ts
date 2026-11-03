import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { ApiRequestError } from '../../core/models';
import { EntradaResponse } from '../../core/entrada-models';
import { EntradasService } from '../../core/entradas.service';
import { OrdenResponse } from '../../core/order-models';
import { OrdersService } from '../../core/orders.service';
import { ProductosService } from '../../core/productos.service';
import { EntradaForm } from './entrada-form';

const order: OrdenResponse = {
  idOrden: 4,
  idProveedor: 1,
  nomProveedor: 'Proveedor',
  prefijoGuia: 'GUIA-TEST-',
  idUsuario: 1,
  nomUsuario: 'Admin',
  fechaEmision: '2026-10-10',
  fechaEstimadaEntrega: null,
  estado: 'Parcial',
  totalOrden: 100,
  detalles: [
    {
      idDetalle: 1,
      idProducto: 7,
      sku: 'CPU-7',
      nomProducto: 'Procesador',
      cantidad: 10,
      cantidadRecibida: 4,
      cantidadPorRecibir: 6,
      precioUnitario: 10,
      subtotal: 100,
    },
  ],
};

const original: EntradaResponse = {
  idEntrada: 8,
  idDocumento: 31,
  idOrden: 4,
  numeroGuiaRemision: 'G-8',
  estadoDocumento: 1,
  idDocumentoOrigen: null,
  observaciones: null,
  fechaEmision: '2026-10-11T10:00:00',
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
      stockActual: 8,
    },
  ],
};

async function setup(edit = false, estado: OrdenResponse['estado'] = 'Parcial') {
  const current = { ...order, estado };

  const entradas = {
    get: vi.fn().mockResolvedValue(original),
    save: vi.fn(),
  };

  const orders = {
    list: vi
      .fn()
      .mockResolvedValue({ content: [current], totalElements: 1, totalPages: 1, number: 0 }),
    get: vi.fn().mockResolvedValue(current),
  };

  const productos = {
    list: vi.fn().mockResolvedValue([
      {
        idProducto: 7,
        sku: 'CPU-7',
        nomProducto: 'Procesador',
        stockActual: 8,
        estado: 1,
      },
    ]),
  };

  const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
  await TestBed.configureTestingModule({
    imports: [EntradaForm],
    providers: [
      provideRouter([]),
      { provide: EntradasService, useValue: entradas },
      { provide: OrdersService, useValue: orders },
      { provide: ProductosService, useValue: productos },
      { provide: AuthService, useValue: { valid: () => true } },
      { provide: MatDialog, useValue: dialog },
      {
        provide: ActivatedRoute,
        useValue: {
          snapshot: {
            paramMap: convertToParamMap(edit ? { id: '8' } : {}),
            queryParamMap: convertToParamMap({}),
          },
        },
      },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(EntradaForm);
  await fixture.componentInstance.load();
  fixture.detectChanges();

  return {
    form: fixture.componentInstance,
    dialog,
    entradas,
    orders,
    fixture,
  };
}

describe('Formulario de Entradas', () => {
  it('rechaza un prefijo de otro proveedor sin enviar la entrada', async () => {
    const { form, entradas } = await setup();

    form.form.controls.idOrden.setValue(4);
    await form.selectOrder(4);
    form.form.controls.numeroGuiaRemision.setValue('GUIA-OTRO-001-123');

    await form.submit();

    expect(entradas.save).not.toHaveBeenCalled();
    expect(form.guiaError()).toContain('debe comenzar con GUIA-TEST-');
  });

  it('revalida la guía si cambia el prefijo de la orden seleccionada', async () => {
    const { form, orders } = await setup();

    form.form.controls.idOrden.setValue(4);
    await form.selectOrder(4);
    form.form.controls.numeroGuiaRemision.setValue('GUIA-TEST-001-123');

    expect(form.form.controls.numeroGuiaRemision.valid).toBe(true);

    orders.get.mockResolvedValueOnce({
      ...order,
      prefijoGuia: 'GUIA-OTRO-',
    });

    await form.selectOrder(4);

    expect(form.form.controls.numeroGuiaRemision.invalid).toBe(true);
  });

  it('mantiene legible una guía histórica al rectificar', async () => {
    const { form } = await setup(true);

    expect(form.form.controls.numeroGuiaRemision.value).toBe('G-8');
    expect(form.form.controls.numeroGuiaRemision.valid).toBe(true);
  });

  it('ofrece solo órdenes que admiten recepciones nuevas y calcula el saldo', async () => {
    const { form } = await setup();

    expect(form.availableOrders().map((item) => item.idOrden)).toEqual([4]);

    form.form.controls.idOrden.setValue(4);
    await form.selectOrder(4);

    expect(form.balance(7)).toBe(6);
    expect(form.lines.at(0).controls.idProducto.value).toBe(7);
    expect(form.orderLine(7)?.cantidadRecibida).toBe(4);
  });

  it('en una rectificación permite reutilizar lo recibido por la original', async () => {
    const { form } = await setup(true);

    expect(form.balance(7)).toBe(8);
    expect(form.form.controls.idOrden.value).toBe(4);
    expect(form.lines.at(0).controls.cantidad.value).toBe(2);
  });

  it('en una orden cancelada impide aumentar o agregar productos a la entrada original', async () => {
    const { form, entradas } = await setup(true, 'Cancelada');

    expect(form.balance(7)).toBe(2);

    form.lines.at(0).controls.cantidad.setValue(3);
    form.form.controls.motivoCorreccion.setValue('Corrección de recepción');

    await form.submit();

    expect(entradas.save).not.toHaveBeenCalled();
    expect(form.error()).toContain('no superar el saldo permitido');
  });

  it('exige motivo separado al rectificar', async () => {
    const { form, entradas } = await setup(true);

    await form.submit();

    expect(entradas.save).not.toHaveBeenCalled();
    expect(form.error()).toContain('campos obligatorios');
  });

  it('conserva la cantidad escrita y muestra el nuevo saldo de la línea tras un rechazo', async () => {
    const { form, entradas, orders } = await setup();

    form.form.controls.idOrden.setValue(4);
    await form.selectOrder(4);
    form.form.controls.numeroGuiaRemision.setValue('GUIA-TEST-9');
    form.lines.at(0).controls.cantidad.setValue(5);

    orders.get.mockResolvedValueOnce({
      ...order,
      detalles: [
        {
          ...order.detalles[0],
          cantidadRecibida: 8,
          cantidadPorRecibir: 2,
        },
      ],
    });

    entradas.save.mockRejectedValueOnce(
      new ApiRequestError(
        'La cantidad recibida supera el saldo de la orden para el producto: 7',
        422,
      ),
    );

    await form.submit();

    expect(form.lines.at(0).controls.cantidad.value).toBe(5);
    expect(form.balance(7)).toBe(2);
    expect(form.lineErrors()[7]).toContain('máximo disponible ahora es 2');
  });
});
describe('Sanitización de textos libres en entradas', () => {
  it('rechaza emojis en observaciones y motivo de corrección', async () => {
    const { form } = await setup();
    form.form.controls.observaciones.setValue('Llegó todo ✅');
    form.form.controls.motivoCorreccion.setValue('Ajuste \u{1F600}');

    expect(form.form.controls.observaciones.hasError('emoji')).toBe(true);
    expect(form.form.controls.motivoCorreccion.hasError('emoji')).toBe(true);
  });

  it('acepta texto normal sin emojis', async () => {
    const { form } = await setup();
    form.form.controls.observaciones.setValue('Llegó completo; se revisó caja por caja.');

    expect(form.form.controls.observaciones.hasError('emoji')).toBe(false);
  });
});

describe('Confirmación D08 de entradas', () => {
  it('cancelar conserva el formulario y no escribe; el resumen muestra stock, producto y cantidad', async () => {
    const { form, entradas, dialog } = await setup();
    form.form.controls.idOrden.setValue(4);
    await form.selectOrder(4);
    form.form.controls.numeroGuiaRemision.setValue('GUIA-TEST-9');
    form.lines.at(0).controls.cantidad.setValue(3);
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    await form.submit();
    expect(entradas.save).not.toHaveBeenCalled();
    expect(form.lines.at(0).controls.cantidad.value).toBe(3);
    expect(dialog.open.mock.calls[0][1].data.inventory).toEqual([
      {
        producto: 'CPU-7 · Procesador · ID 7',
        stockPrevio: 8,
        reversion: 0,
        cantidad: 3,
        cambioNeto: 3,
        stockResultado: 11,
      },
    ]);
    expect(form.busy()).toBe(false);
  });

  it('rectificación resume original y reemplazo e impide otra apertura mientras espera', async () => {
    const { form, entradas, dialog } = await setup(true);
    form.form.controls.motivoCorreccion.setValue('Cantidad corregida');
    form.lines.at(0).controls.cantidad.setValue(3);
    const answer = new Subject<boolean>();
    dialog.open.mockReturnValue({ afterClosed: () => answer });
    const pending = form.submit();
    await form.submit();
    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(dialog.open.mock.calls[0][1].data.inventory[0]).toMatchObject({
      reversion: 2,
      cantidad: 3,
      stockPrevio: 8,
      cambioNeto: 1,
      stockResultado: 9,
    });
    expect(entradas.save).not.toHaveBeenCalled();
    answer.next(false);
    answer.complete();
    await pending;
    expect(form.busy()).toBe(false);
  });

  it('el selector carga otra página solo por acción explícita', async () => {
    const { form, orders } = await setup();
    orders.list.mockResolvedValueOnce({
      content: [{ ...order, idOrden: 20 }],
      totalElements: 21,
      totalPages: 2,
      number: 1,
    });
    await form.loadOrderOptions();
    expect(orders.list).toHaveBeenLastCalledWith({ page: 1, size: 20, q: '', admisibles: true });
    expect(form.ordersList().map((item) => item.idOrden)).toEqual([4, 20]);
  });

  it('la última búsqueda de órdenes prevalece sobre una respuesta anterior pendiente', async () => {
    const { form, orders } = await setup();
    type PaginaOrdenes = Awaited<ReturnType<OrdersService['list']>>;
    let finishFirst!: (page: PaginaOrdenes) => void;
    let finishLatest!: (page: PaginaOrdenes) => void;
    orders.list
      .mockReturnValueOnce(new Promise<PaginaOrdenes>((resolve) => (finishFirst = resolve)))
      .mockReturnValueOnce(new Promise<PaginaOrdenes>((resolve) => (finishLatest = resolve)));

    const first = form.searchOrders('Proveedor A');
    const latest = form.searchOrders('Proveedor B');
    finishLatest({
      content: [{ ...order, idOrden: 22 }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    });
    await latest;
    finishFirst({
      content: [{ ...order, idOrden: 11 }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
    });
    await first;

    expect(orders.list).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      q: 'Proveedor B',
      admisibles: true,
    });
    expect(form.ordersList().map((item) => item.idOrden)).toEqual([22]);
    expect(form.orderPage()).toBe(0);
    expect(form.loadingOrders()).toBe(false);
  });
});
