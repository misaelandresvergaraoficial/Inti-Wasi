import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { UsersService } from '../../core/users.service';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { ApiRequestError, Rol } from '../../core/models';
import { PendingChanges } from '../../core/guards';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { fieldError, focusFirstInvalid } from '../../shared/form-errors';
import { Icon } from '../../shared/icon';
const notBlank = (control: AbstractControl) =>
  typeof control.value === 'string' && !control.value.trim() ? { blank: true } : null;
@Component({
  selector: 'iw-user-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    Icon,
  ],
  templateUrl: './user-form.html',
})
export class UserForm implements PendingChanges {
  private readonly users = inject(UsersService);
  private readonly auth = inject(AuthService);
  private readonly notice = inject(NoticeService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly element = viewChild<ElementRef<HTMLFormElement>>('userForm');
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id')) || undefined;
  readonly form = inject(FormBuilder).nonNullable.group({
    nomUsuario: ['', [Validators.required, notBlank, Validators.maxLength(50)]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    contrasena: ['', [Validators.minLength(6), Validators.maxLength(20)]],
    rol: ['Operador de Almacén' as Rol, Validators.required],
    telefono: ['', Validators.maxLength(15)],
  });
  readonly busy = signal(false);
  readonly loading = signal(!!this.id);
  readonly error = signal('');
  readonly loadError = signal('');
  readonly showPassword = signal(false);
  readonly errorText = fieldError;
  constructor() {
    if (!this.id) this.form.controls.contrasena.addValidators([Validators.required, notBlank]);
    else void this.load();
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.loadError.set('');
    try {
      const user = await this.users.get(this.id!);
      this.form.patchValue({ ...user, telefono: user.telefono ?? '', contrasena: '' });
      this.form.markAsPristine();
    } catch (error) {
      this.loadError.set(error instanceof Error ? error.message : 'No se pudo cargar el usuario.');
    } finally {
      this.loading.set(false);
    }
  }
  async submit(): Promise<void> {
    if (this.busy()) return;
    this.error.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      focusFirstInvalid(this.element()!.nativeElement);
      return;
    }
    this.busy.set(true);
    try {
      const value = this.form.getRawValue();
      const user = await this.users.save(
        { ...value, telefono: value.telefono || null, contrasena: value.contrasena || undefined },
        this.id,
      );
      this.form.markAsPristine();
      const current = this.auth.session();
      if (
        current?.idUsuario === user.idUsuario &&
        (current.correo !== user.correo || !!value.contrasena)
      ) {
        this.auth.logout(false, true);
        return;
      }
      this.auth.refreshUser(user);
      this.notice.show(
        `${user.nomUsuario}: ${this.id ? 'cambios guardados' : 'usuario creado correctamente'}.`,
      );
      await this.router.navigate([this.auth.isAdmin() ? '/usuarios' : '/inicio']);
    } catch (error) {
      if (
        error instanceof ApiRequestError &&
        error.field &&
        error.field in this.form.controls
      ) {
        this.form.controls[error.field as keyof typeof this.form.controls].setErrors({
          server: error.message,
        });
        focusFirstInvalid(this.element()!.nativeElement);
      }
      this.error.set(
        error instanceof Error ? error.message : 'No se pudo guardar. Inténtalo de nuevo.',
      );
    } finally {
      this.busy.set(false);
    }
  }
  async canLeave(): Promise<boolean> {
    if (!this.auth.valid() || !this.form.dirty) return true;
    if (this.busy()) return false;
    return !!(await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '460px',
          maxWidth: 'calc(100vw - 32px)',
          data: {
            title: '¿Salir sin guardar?',
            message: 'Los cambios que realizaste en este formulario se perderán.',
            action: 'Salir sin guardar',
          },
        })
        .afterClosed(),
    ));
  }
}
