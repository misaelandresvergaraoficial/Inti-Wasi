import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it, vi } from 'vitest';
import { UsersService } from '../../core/users.service';
import { setupForm } from '../../shared/spec-helpers';
import { UserForm, institutionalEmail } from './user-form';

async function setupUserForm() {
  return setupForm(UserForm, [
    { provide: UsersService, useValue: { get: vi.fn(), save: vi.fn() } },
  ]);
}

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

describe('Sanitización del formulario de usuarios', () => {
  it('rechaza el nombre con números o emojis', async () => {
    const form = await setupUserForm();
    for (const value of ['Misael 2026', 'Misael 😀', 'Misael_Vergara']) {
      form.controls.nomUsuario.setValue(value);
      expect(form.controls.nomUsuario.hasError('pattern')).toBe(true);
    }
  });

  it('acepta el nombre real con tildes y espacios', async () => {
    const form = await setupUserForm();
    form.controls.nomUsuario.setValue('Misael Andrés Vergara Morales');

    expect(form.controls.nomUsuario.hasError('pattern')).toBe(false);
  });

  it('rechaza teléfonos de 8 o 10 dígitos', async () => {
    const form = await setupUserForm();
    for (const value of ['98765432', '9876543210']) {
      form.controls.telefono.setValue(value);
      expect(form.controls.telefono.hasError('pattern')).toBe(true);
    }
  });

  it('rechaza un teléfono que no empieza con 9', async () => {
    const form = await setupUserForm();
    form.controls.telefono.setValue('879348552');

    expect(form.controls.telefono.hasError('pattern')).toBe(true);
  });

  it('acepta un teléfono de 9 dígitos que empieza con 9', async () => {
    const form = await setupUserForm();
    form.controls.telefono.setValue('979348552');

    expect(form.controls.telefono.hasError('pattern')).toBe(false);
  });
});
