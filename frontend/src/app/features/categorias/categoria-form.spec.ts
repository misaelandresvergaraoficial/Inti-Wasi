import { describe, expect, it, vi } from 'vitest';
import { CategoriasService } from '../../core/categorias.service';
import { setupForm } from '../../shared/spec-helpers';
import { CategoriaForm } from './categoria-form';

async function setup() {
  return setupForm(CategoriaForm, [
    { provide: CategoriasService, useValue: { get: vi.fn(), save: vi.fn() } },
  ]);
}

describe('Sanitización del formulario de categorías', () => {
  it('rechaza emojis, guiones bajos y signos no permitidos', async () => {
    const form = await setup();
    for (const value of ['Periféricos ✅', 'Memorias_RAM', 'Fuentes @ Poder', 'Redes 100%']) {
      form.controls.nomCategoria.setValue(value);
      expect(form.controls.nomCategoria.hasError('pattern')).toBe(true);
    }
  });

  it('acepta la categoría real con paréntesis y barra', async () => {
    const form = await setup();
    form.controls.nomCategoria.setValue('Almacenamiento (SSD / HDD)');

    expect(form.controls.nomCategoria.hasError('pattern')).toBe(false);
  });
});
