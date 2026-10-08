import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { PendingChanges } from '../../core/guards';
import { NoticeService } from '../../core/notice.service';
import { ApiRequestError } from '../../core/models';
import { ProveedoresService } from '../../core/proveedores.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { fieldError, focusFirstInvalid } from '../../shared/form-errors';
import { Icon } from '../../shared/icon';

const notBlank = (control: AbstractControl) =>
  typeof control.value === 'string' && !control.value.trim() ? { blank: true } : null;

@Component({
  selector: 'iw-proveedor-form',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, Icon],
  templateUrl: './proveedor-form.html',
})
export class ProveedorForm implements PendingChanges {
  private readonly suppliers = inject(ProveedoresService);
  private readonly auth = inject(AuthService);
  private readonly notice = inject(NoticeService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly element = viewChild<ElementRef<HTMLFormElement>>('supplierForm');
  private readonly fb = inject(FormBuilder);
  private readonly rawId = inject(ActivatedRoute).snapshot.paramMap.get('id');

  readonly id = this.rawId === null ? undefined : Number(this.rawId);
  readonly invalidId =
    this.rawId !== null && (!Number.isInteger(this.id) || (this.id as number) <= 0);
  readonly form = this.fb.nonNullable.group({
    nomProveedor: ['', [Validators.required, notBlank, Validators.maxLength(100)]],
    ruc: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    contacto: ['', [Validators.maxLength(100)]],
    telefono: ['', [Validators.required, notBlank, Validators.maxLength(20)]],
    direccion: ['', [Validators.maxLength(150)]],
  });
  readonly busy = signal(false);
  readonly loading = signal(this.rawId !== null && !this.invalidId);
  readonly error = signal('');
  readonly loadError = signal(this.invalidId ? 'El identificador del proveedor no es válido.' : '');
  readonly errorText = fieldError;

  constructor() {
    if (this.id && !this.invalidId) void this.load();
  }

  async load(): Promise<void> {
    if (!this.id) return;
    this.loading.set(true);
    this.loadError.set('');
    try {
      const supplier = await this.suppliers.get(this.id);
      this.form.patchValue({
        nomProveedor: supplier.nomProveedor,
        ruc: supplier.ruc,
        contacto: supplier.contacto ?? '',
        telefono: supplier.telefono,
        direccion: supplier.direccion ?? '',
      });
      this.form.markAsPristine();
    } catch (error) {
      this.loadError.set(
        error instanceof Error ? error.message : 'No se pudo cargar la información del proveedor.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    if (this.busy() || this.loading() || this.loadError()) return;
    this.error.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      if (this.element()) focusFirstInvalid(this.element()!.nativeElement);
      return;
    }

    this.busy.set(true);
    try {
      const value = this.form.getRawValue();
      const supplier = await this.suppliers.save(
        {
          nomProveedor: value.nomProveedor.trim(),
          ruc: value.ruc.trim(),
          contacto: value.contacto.trim() || null,
          telefono: value.telefono.trim(),
          direccion: value.direccion.trim() || null,
        },
        this.id,
      );
      this.form.markAsPristine();
      this.notice.show(
        `${supplier.nomProveedor}: ${this.id ? 'cambios guardados' : 'proveedor creado correctamente'}.`,
      );
      await this.router.navigate(['/proveedores']);
    } catch (error) {
      if (error instanceof ApiRequestError && error.field && error.field in this.form.controls) {
        const control = this.form.controls[error.field as keyof typeof this.form.controls];
        control.setErrors({ ...control.errors, server: error.message });
        if (this.element()) focusFirstInvalid(this.element()!.nativeElement);
      }
      this.error.set(error instanceof Error ? error.message : 'No se pudo guardar el proveedor.');
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
