import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { of, Subject } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ApiRequestError } from '../../core/models';
import { ProductoResponse } from '../../core/producto-models';
import { ProductosService } from '../../core/productos.service';
import { SalidaResponse } from '../../core/salida-models';
import { SalidasService } from '../../core/salidas.service';
import { SalidaForm } from './salida-form';

const products: ProductoResponse[] = [
  {
    idProducto: 7,
    sku: 'CPU-7',
    nomProducto: 'Procesador',
    idCategoria: 1,
    nomCategoria: 'Componentes',
    precio: 10,
    stockMinimo: 1,
    stockActual: 3,
    estado: 1,
  },
];

const original: SalidaResponse = {
  idSalida: 5,
  idDocumento: 22,
  estadoDocumento: 1,
  idDocumentoOrigen: null,
  motivo: 'Merma',
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

async function setup(edit = false) {
  const salidas = { get: vi.fn().mockResolvedValue(original), save: vi.fn() };
  const dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(true) }) };
  const productos = { list: vi.fn().mockResolvedValue(products) };
  await TestBed.configureTestingModule({
    imports: [SalidaForm],
    providers: [
      provideRouter([]),
      { provide: SalidasService, useValue: salidas },
      { provide: ProductosService, useValue: productos },
      { provide: AuthService, useValue: { valid: () => true } },
      { provide: MatDialog, useValue: dialog },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { paramMap: convertToParamMap(edit ? { id: '5' } : {}) } },
      },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(SalidaForm);
  await fixture.componentInstance.load();
  fixture.detectChanges();
  return { form: fixture.componentInstance, salidas, productos, dialog };
}

describe('Formulario de Salidas', () => {
  it('conserva la prohibición de emojis al cambiar el motivo', async () => {
    const { form } = await setup();
    form.form.controls.observaciones.setValue('Conteo 😀');
    for (const motivo of ['Otro', 'Merma', 'Despacho/Venta']) {
      form.form.controls.motivo.setValue(motivo);
      expect(form.form.controls.observaciones.hasError('emoji')).toBe(true);
    }
  });

  it('limita una salida nueva al stock actual', async () => {
    const { form, salidas } = await setup();
    form.form.controls.motivo.setValue('Merma');
    form.lines.at(0).controls.idProducto.setValue(7);
    form.lines.at(0).controls.cantidad.setValue(4);
    expect(form.availableStock(7)).toBe(3);
    await form.submit();
    expect(salidas.save).not.toHaveBeenCalled();
    expect(form.error()).toContain('stock suficiente');
  });

  it('al rectificar considera la reversión completa de la salida original', async () => {
    const { form } = await setup(true);
    expect(form.availableStock(7)).toBe(5);
    expect(form.currentStock(7)).toBe(3);
    expect(form.resultingStock(7, 2)).toBe(3);
    expect(form.lines.at(0).controls.cantidad.value).toBe(2);
  });

  it('exige motivo de corrección distinto del motivo de salida', async () => {
    const { form, salidas } = await setup(true);
    expect(form.form.controls.motivo.value).toBe('Merma');
    await form.submit();
    expect(salidas.save).not.toHaveBeenCalled();
    expect(form.error()).toContain('campos obligatorios');
  });

  it('impide repetir un producto en dos líneas', async () => {
    const { form, salidas } = await setup();
    form.form.controls.motivo.setValue('Otro');
    form.form.controls.observaciones.setValue('Salida extraordinaria');
    form.lines.at(0).controls.idProducto.setValue(7);
    form.addLine();
    form.lines.at(1).controls.idProducto.setValue(7);
    await form.submit();
    expect(salidas.save).not.toHaveBeenCalled();
    expect(form.duplicate(1)).toBe(true);
  });

  it('exige observaciones cuando el motivo es Otro', async () => {
    const { form, salidas } = await setup();
    form.form.controls.motivo.setValue('Otro');
    form.lines.at(0).controls.idProducto.setValue(7);
    await form.submit();
    expect(salidas.save).not.toHaveBeenCalled();
    expect(form.form.controls.observaciones.invalid).toBe(true);
  });

  it('mantiene fijos los productos de la salida original al rectificar', async () => {
    const { form } = await setup(true);
    form.addLine();
    expect(form.lines.length).toBe(1);
    expect(form.lines.at(0).controls.idProducto.disabled).toBe(true);
    expect(form.lines.at(0).controls.idProducto.value).toBe(7);
  });

  it('mantiene lo escrito y actualiza el stock después de un rechazo concurrente', async () => {
    const { form, salidas, productos } = await setup();
    form.form.controls.motivo.setValue('Merma');
    form.lines.at(0).controls.idProducto.setValue(7);
    form.lines.at(0).controls.cantidad.setValue(2);
    productos.list.mockResolvedValueOnce([{ ...products[0], stockActual: 1 }]);
    salidas.save.mockRejectedValueOnce(
      new ApiRequestError('Stock insuficiente para el producto: 7', 422),
    );
    await form.submit();
    expect(form.lines.at(0).controls.cantidad.value).toBe(2);
    expect(form.currentStock(7)).toBe(1);
    expect(form.resultingStock(7, 2)).toBe(-1);
    expect(form.lineErrors()[7]).toContain('Solo hay 1');
  });

  it('cancelar la confirmación no envía escritura y muestra efecto neto', async () => {
    const { form, salidas, dialog } = await setup(true);
    form.form.controls.motivoCorreccion.setValue('Rectificar cantidad');
    form.lines.at(0).controls.cantidad.setValue(4);
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });
    await form.submit();
    expect(salidas.save).not.toHaveBeenCalled();
    expect(dialog.open.mock.calls[0][1].data.inventory[0]).toMatchObject({
      stockPrevio: 3,
      cantidad: 4,
      reversion: 2,
      cambioNeto: -2,
      stockResultado: 1,
    });
  });

  it('bloquea un segundo envío mientras la confirmación permanece abierta', async () => {
    const { form, salidas, dialog } = await setup();
    form.form.controls.motivo.setValue('Merma');
    form.lines.at(0).controls.idProducto.setValue(7);
    const closed = new Subject<boolean>();
    dialog.open.mockReturnValue({ afterClosed: () => closed });
    const pending = form.submit();
    await form.submit();
    expect(dialog.open).toHaveBeenCalledTimes(1);
    closed.next(false);
    closed.complete();
    await pending;
    expect(salidas.save).not.toHaveBeenCalled();
  });
});
