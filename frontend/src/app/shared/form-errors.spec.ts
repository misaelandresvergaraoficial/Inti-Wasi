import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { nombrePersonal, normalizarNombre, tieneEmojis } from './form-errors';

describe('Nombres y motivos compartidos', () => {
  it('acepta nombres legítimos y normaliza espacios exteriores y Unicode', () => {
    const value = '  Mari\u0301a-José D’Angelo  ';
    expect(new FormControl(value, nombrePersonal(50)).valid).toBe(true);
    expect(normalizarNombre(value)).toBe('María-José D’Angelo');
    expect(new FormControl("Ana O'Neill", nombrePersonal(100, true)).valid).toBe(true);
    expect(new FormControl('Ana \u{10400}\u{10428}', nombrePersonal(50)).valid).toBe(true);
    expect(new FormControl(' ', nombrePersonal(100, true)).valid).toBe(true);
  });

  it('rechaza números, emojis y nombres vacíos', () => {
    expect(new FormControl('Ana 5', nombrePersonal(50)).hasError('pattern')).toBe(true);
    expect(new FormControl('Ana 😀', nombrePersonal(50)).hasError('emoji')).toBe(true);
    expect(new FormControl(' ', nombrePersonal(50)).valid).toBe(false);
    expect(tieneEmojis('Rectificación 😀')).toBe(true);
    expect(tieneEmojis('Rectificación por conteo físico.')).toBe(false);
    expect(tieneEmojis('Conteo \u{10400}\u{10428}')).toBe(false);
    expect(tieneEmojis('Dato \uD800')).toBe(true);
  });
});
