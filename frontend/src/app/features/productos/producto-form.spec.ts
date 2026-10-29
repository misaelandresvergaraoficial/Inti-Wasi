import { describe, expect, it, vi } from 'vitest';
import { ProductosService } from '../../core/productos.service';
import { setupForm } from '../../shared/spec-helpers';
import { ProductoForm } from './producto-form';

async function setup() {
  return setupForm(ProductoForm, [
    {
      provide: ProductosService,
      useValue: {
        get: vi.fn(),
        save: vi.fn(),
        getCategorias: vi.fn().mockResolvedValue([]),
        getProveedores: vi.fn().mockResolvedValue([]),
      },
    },
  ]);
}

describe('Sanitización del formulario de productos', () => {
  it('rechaza el nombre con emojis o guion bajo', async () => {
    const form = await setup();
    for (const value of ['Mouse Gamer 😀', 'Cable_HDMI', 'Monitor 24" ✅']) {
      form.controls.nomProducto.setValue(value);
      expect(form.controls.nomProducto.hasError('pattern')).toBe(true);
    }
  });

  it('acepta el nombre técnico con signos permitidos', async () => {
    const form = await setup();
    for (const value of [
      'Cable HDMI 2.1 & Adaptador (3 m)',
      'Memoria RAM Kingston Fury Beast 16GB (1x16GB) DDR4 3200MHz',
      'Teclado Mecánico Redragon Kumara K552 RGB Switch Blue',
    ]) {
      form.controls.nomProducto.setValue(value);
      expect(form.controls.nomProducto.hasError('pattern')).toBe(false);
    }
  });

  it('rechaza precios negativos y acepta el cero', async () => {
    const form = await setup();
    form.controls.precio.setValue(-1);
    expect(form.controls.precio.hasError('min')).toBe(true);

    form.controls.precio.setValue(0);
    expect(form.controls.precio.hasError('min')).toBe(false);
    expect(form.controls.precio.hasError('pattern')).toBe(false);
  });

  it('acepta un precio desde 0.01', async () => {
    const form = await setup();
    form.controls.precio.setValue(0.01);

    expect(form.controls.precio.hasError('min')).toBe(false);
  });

  it('rechaza el stock mínimo negativo y acepta cero', async () => {
    const form = await setup();
    form.controls.stockMinimo.setValue(-1);
    expect(form.controls.stockMinimo.hasError('min')).toBe(true);

    form.controls.stockMinimo.setValue(0);
    expect(form.controls.stockMinimo.hasError('min')).toBe(false);
  });
});
