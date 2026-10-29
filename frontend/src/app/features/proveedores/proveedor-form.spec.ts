import { describe, expect, it, vi } from 'vitest';
import { ProveedoresService } from '../../core/proveedores.service';
import { setupForm } from '../../shared/spec-helpers';
import { ProveedorForm } from './proveedor-form';

async function setup() {
  return setupForm(ProveedorForm, [
    { provide: ProveedoresService, useValue: { get: vi.fn(), save: vi.fn() } },
  ]);
}

describe('Sanitización del formulario de proveedores', () => {
  it('admite números en la razón social, pero no en el contacto', async () => {
    const form = await setup();
    form.controls.nomProveedor.setValue('Distribuidora 2026 S.A.C.');
    form.controls.contacto.setValue('Jorge 2');

    expect(form.controls.nomProveedor.hasError('pattern')).toBe(false);
    expect(form.controls.contacto.hasError('pattern')).toBe(true);
  });

  it('acepta la razón social real con tildes y puntos', async () => {
    const form = await setup();
    form.controls.nomProveedor.setValue('Ingram Micro Perú S.A.C.');

    expect(form.controls.nomProveedor.hasError('pattern')).toBe(false);
  });

  it('rechaza el contacto con números o signos', async () => {
    const form = await setup();
    for (const value of ['Jorge 2', 'Jorge_Del_Solar', 'Jorge 😀']) {
      form.controls.contacto.setValue(value);
      expect(form.controls.contacto.hasError('pattern')).toBe(true);
    }
  });

  it('acepta el contacto escrito solo con letras y espacios', async () => {
    const form = await setup();
    form.controls.contacto.setValue('Mariana Falcón');

    expect(form.controls.contacto.hasError('pattern')).toBe(false);
  });

  it('rechaza teléfonos de 8 o 10 dígitos', async () => {
    const form = await setup();
    for (const value of ['01415000', '0141500001']) {
      form.controls.telefono.setValue(value);
      expect(form.controls.telefono.hasError('pattern')).toBe(true);
    }
  });

  it('acepta celular y fijo nacional de 9 dígitos', async () => {
    const form = await setup();

    for (const value of ['979348552', '014150000']) {
      form.controls.telefono.setValue(value);
      expect(form.controls.telefono.hasError('pattern')).toBe(false);
    }
  });
});
