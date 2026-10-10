import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { CategoriasService } from '../../core/categorias.service';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { PendingChanges } from '../../core/guards';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

const notBlank = (control: AbstractControl) =>
  typeof control.value === 'string' && !control.value.trim() ? { blank: true } : null;

@Component({
  selector: 'iw-categoria-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    Icon,
  ],
  templateUrl: './categoria-form.html',
})
export class CategoriaForm implements PendingChanges {
  private readonly element = viewChild<ElementRef<HTMLFormElement>>('categoriaForm');
  private readonly fb = inject(FormBuilder);
  private readonly srv = inject(CategoriasService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  readonly isEdit = signal(false);
  readonly categoryIdDisplay = signal<number | null>(null);
  readonly saving = signal(false);
  readonly loading = signal(true);
  readonly errorInit = signal('');
  readonly error = signal('');

  private categoryId?: number;
  private originalEstado?: number;

  readonly form = this.fb.nonNullable.group({
    nomCategoria: ['', [Validators.required, notBlank, Validators.maxLength(100)]],
    estado: [1, Validators.required],
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      const parsed = Number(id);
      if (Number.isInteger(parsed) && parsed > 0) this.categoryId = parsed;
    }
    void this.init();
  }

  async canLeave(): Promise<boolean> {
    if (!this.auth.valid() || !this.form.dirty) return true;
    if (this.saving()) return false;
    return !!(await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '460px',
          maxWidth: 'calc(100vw - 32px)',
          data: {
            title: '¿Salir sin guardar?',
            message: 'Los cambios que realizaste en esta categoría se perderán.',
            action: 'Salir sin guardar',
          },
        })
        .afterClosed(),
    ));
  }

  async init(): Promise<void> {
    this.loading.set(true);
    this.errorInit.set('');
    if (this.isEdit() && !this.categoryId) {
      this.errorInit.set('El identificador de la categoría no es válido.');
      this.loading.set(false);
      return;
    }
    try {
      if (this.categoryId) {
        const c = await this.srv.get(this.categoryId);
        this.originalEstado = c.estado;
        this.categoryIdDisplay.set(c.idCategoria);
        this.form.patchValue({
          nomCategoria: c.nomCategoria,
          estado: c.estado,
        });
      }
      this.form.markAsPristine();
    } catch (err) {
      this.errorInit.set(err instanceof Error ? err.message : 'No se pudo cargar la categoría.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    if (this.saving() || this.loading() || this.errorInit()) return;
    this.error.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      requestAnimationFrame(() =>
        this.element()
          ?.nativeElement.querySelector<HTMLElement>('input.ng-invalid, mat-select.ng-invalid')
          ?.focus(),
      );
      return;
    }
    this.saving.set(true);
    try {
      const value = this.form.getRawValue();
      const willDeactivateProducts =
        this.isEdit() && this.originalEstado === 1 && value.estado === 0;
      if (willDeactivateProducts) {
        const confirmed = await firstValueFrom(
          this.dialog
            .open(ConfirmDialog, {
              width: '480px',
              maxWidth: 'calc(100vw - 32px)',
              data: {
                title: '¿Desactivar la categoría?',
                message:
                  'También se desactivarán sus productos activos. Al reactivar la categoría, deberás reactivar cada producto por separado.',
                action: 'Desactivar categoría y productos',
                danger: true,
              },
            })
            .afterClosed(),
        );
        if (!confirmed) return;
      }
      const categoria = await this.srv.save(
        {
          ...value,
          nomCategoria: value.nomCategoria.trim(),
        },
        this.categoryId,
      );
      this.form.markAsPristine();
      this.notice.show(
        `${categoria.nomCategoria}: ${
          this.isEdit() ? 'cambios guardados' : 'categoría creada correctamente'
        }.${willDeactivateProducts ? ' Los productos activos de esta categoría se desactivaron.' : ''}`,
      );
      await this.router.navigate(['/categorias']);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Error de validación o conflicto.');
    } finally {
      this.saving.set(false);
    }
  }
}
