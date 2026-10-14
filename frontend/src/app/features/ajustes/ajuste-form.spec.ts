import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AjusteResponse } from '../../core/ajuste-models';
import { AjustesService } from '../../core/ajustes.service';
import { AuthService } from '../../core/auth.service';
import { ProductoResponse } from '../../core/producto-models';
import { ProductosService } from '../../core/productos.service';
import { AjusteForm } from './ajuste-form';

const original: AjusteResponse = {
  idAjuste: 3,
  idDocumento: 18,
  estadoDocumento: 1,
  idDocumentoOrigen: null,
  tipoAjuste: 'Incremento',
  motivo: 'Diferencia física',
  observaciones: null,
  fechaEmision: '2026-10-13T11:00:00',
  idUsuario: 1,
  usuarioResponsable: 'Administradora',
  movimientos: [
    {
      idMovimiento: 1,
      idProducto: 7,
      sku: 'CPU-7',
      nomProducto: 'Procesador',
      cantidad: 5,
      stockActual: 1,
    },
  ],
};
const product = (stockActual: number): ProductoResponse[] => [
  {
    idProducto: 7,
    sku: 'CPU-7',
    nomProducto: 'Procesador',
    idCategoria: 1,
    nomCategoria: 'Componentes',
    precio: 10,
    stockMinimo: 1,
    stockActual,
    estado: 1,
  },
];

async function setup(existing?: AjusteResponse, stock = 1) {
  const ajustes = { get: vi.fn().mockResolvedValue(existing), save: vi.fn() };
  await TestBed.configureTestingModule({
    imports: [AjusteForm],
    providers: [
      provideRouter([]),
      { provide: AjustesService, useValue: ajustes },
      { provide: ProductosService, useValue: { list: vi.fn().mockResolvedValue(product(stock)) } },
      { provide: AuthService, useValue: { valid: () => true } },
      { provide: MatDialog, useValue: {} },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { paramMap: convertToParamMap(existing ? { id: '3' } : {}) } },
      },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(AjusteForm);
  await fixture.componentInstance.load();
  fixture.detectChanges();
  return { form: fixture.componentInstance, ajustes };
}

describe('Formulario de Ajustes', () => {
  it('rechaza un Decremento que supera el stock actual', async () => {
    const { form, ajustes } = await setup();
    form.form.controls.tipoAjuste.setValue('Decremento');
    form.form.controls.motivo.setValue('Conteo físico');
    form.lines.at(0).controls.idProducto.setValue(7);
    form.lines.at(0).controls.cantidad.setValue(2);
    await form.submit();
    expect(ajustes.save).not.toHaveBeenCalled();
    expect(form.error()).toContain('no superar el stock disponible');
  });

  it('rechaza la rectificación si no se puede revertir el Incremento original', async () => {
    const { form, ajustes } = await setup(original);
    expect(form.stockAfterReversal(7)).toBe(-4);
    expect(form.reversalPossible()).toBe(false);
    form.form.controls.motivoCorreccion.setValue('Conteo corregido');
    await form.submit();
    expect(ajustes.save).not.toHaveBeenCalled();
    expect(form.error()).toContain('no alcanza para revertir');
  });

  it('tras revertir un Decremento, calcula el stock disponible para el reemplazo', async () => {
    const { form } = await setup({ ...original, tipoAjuste: 'Decremento' });
    expect(form.stockAfterReversal(7)).toBe(6);
    expect(form.reversalPossible()).toBe(true);
  });

  it('exige motivo de corrección independiente del motivo del ajuste', async () => {
    const { form, ajustes } = await setup({ ...original, tipoAjuste: 'Decremento' });
    expect(form.form.controls.motivo.value).toBe('Diferencia física');
    await form.submit();
    expect(ajustes.save).not.toHaveBeenCalled();
    expect(form.error()).toContain('Completa el tipo');
  });
});
