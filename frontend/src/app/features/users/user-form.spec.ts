import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { institutionalEmail } from './user-form';

describe('Correo institucional de usuarios', () => {
  it('rechaza dominios escritos en la parte editable y nombres vacíos', () => {
    for (const value of ['admin.nombre@otrocorreo.pe', 'admin.', 'nombre', 'admin..nombre', 'operador.carlos.mendoza', 'operador.carlos1', 'operador.carlos_m']) {
      expect(new FormControl(value, institutionalEmail).hasError('institutional')).toBe(true);
    }
  });
  it('acepta añadir la inicial del apellido cuando el nombre ya está ocupado', () => {
    const form = new FormGroup({
      correo: new FormControl('operador.carlosm', institutionalEmail),
      rol: new FormControl('Operador de Almacén'),
    });
    form.controls.correo.updateValueAndValidity();
    expect(form.controls.correo.valid).toBe(true);
  });
  it('comprueba la correspondencia al cambiar de rol', () => {
    const form = new FormGroup({
      correo: new FormControl('operador.nombre', institutionalEmail),
      rol: new FormControl('Operador de Almacén'),
    });
    form.controls.correo.updateValueAndValidity();
    expect(form.controls.correo.valid).toBe(true);
    form.controls.rol.setValue('Administrador');
    form.controls.correo.updateValueAndValidity();
    expect(form.controls.correo.hasError('roleEmail')).toBe(true);
    form.controls.correo.setValue('admin.nombre');
    expect(form.controls.correo.valid).toBe(true);
  });
});
