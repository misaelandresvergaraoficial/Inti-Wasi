import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
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
  const entradas = { get: vi.fn().mockResolvedValue(original), save: vi.fn() };
  const orders = {
    list: vi.fn().mockResolvedValue([current]),
    get: vi.fn().mockResolvedValue(current),
  };
  const productos = {
    list: vi
      .fn()
      .mockResolvedValue([{ idProducto: 7, sku: 'CPU-7', nomProducto: 'Procesador', estado: 1 }]),
  };
  await TestBed.configureTestingModule({
    imports: [EntradaForm],
    providers: [
      provideRouter([]),
      { provide: EntradasService, useValue: entradas },
      { provide: OrdersService, useValue: orders },
      { provide: ProductosService, useValue: productos },
      { provide: AuthService, useValue: { valid: () => true } },
      { provide: MatDialog, useValue: {} },
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
  return { form: fixture.componentInstance, entradas, fixture };
}

describe('Formulario de Entradas', () => {
  it('ofrece solo órdenes que admiten recepciones nuevas y calcula el saldo', async () => {
    const { form } = await setup();
    expect(form.availableOrders().map((item) => item.idOrden)).toEqual([4]);
    form.form.controls.idOrden.setValue(4);
    await form.selectOrder(4);
    expect(form.balance(7)).toBe(6);
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
});
