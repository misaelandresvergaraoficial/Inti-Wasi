import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
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
  await TestBed.configureTestingModule({
    imports: [SalidaForm],
    providers: [
      provideRouter([]),
      { provide: SalidasService, useValue: salidas },
      { provide: ProductosService, useValue: { list: vi.fn().mockResolvedValue(products) } },
      { provide: AuthService, useValue: { valid: () => true } },
      { provide: MatDialog, useValue: {} },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { paramMap: convertToParamMap(edit ? { id: '5' } : {}) } },
      },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(SalidaForm);
  await fixture.componentInstance.load();
  fixture.detectChanges();
  return { form: fixture.componentInstance, salidas };
}

describe('Formulario de Salidas', () => {
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
    form.lines.at(0).controls.idProducto.setValue(7);
    form.addLine();
    form.lines.at(1).controls.idProducto.setValue(7);
    await form.submit();
    expect(salidas.save).not.toHaveBeenCalled();
    expect(form.duplicate(1)).toBe(true);
  });
});
