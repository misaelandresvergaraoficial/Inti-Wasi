import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { ProductosService } from '../../core/productos.service';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { PendingChanges } from '../../core/guards';
import { CategoriaResponse, ProveedorResponse } from '../../core/producto-models';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

const notBlank = (control: AbstractControl) =>
  typeof control.value === 'string' && !control.value.trim() ? { blank: true } : null;

@Component({
  selector: 'iw-producto-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    Icon,
  ],
  templateUrl: './producto-form.html',
})
export class ProductoForm implements PendingChanges {
  private readonly element = viewChild<ElementRef<HTMLFormElement>>('productForm');
  private readonly fb = inject(FormBuilder);
  private readonly srv = inject(ProductosService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  readonly isEdit = signal(false);
  readonly saving = signal(false);
  readonly loading = signal(true);
  readonly errorInit = signal('');
  readonly error = signal('');
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly proveedores = signal<ProveedorResponse[]>([]);

  private productId?: number;

  readonly form = this.fb.nonNullable.group({
    sku: ['', [Validators.required, notBlank, Validators.maxLength(30)]],
    nomProducto: ['', [Validators.required, notBlank, Validators.maxLength(150)]],
    idCategoria: [0, [Validators.required, Validators.min(1)]],
    idProveedor: [null as number | null],
    precio: [
      0,
      [Validators.required, Validators.min(0), Validators.pattern(/^\d{1,8}(\.\d{1,2})?$/)],
    ],
    stockMinimo: [
      0,
      [
        Validators.required,
        Validators.min(0),
        Validators.pattern(/^\d+$/),
        Validators.max(2147483647),
      ],
    ],
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      const parsed = Number(id);
      if (Number.isInteger(parsed) && parsed > 0) this.productId = parsed;
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
            message: 'Los cambios que realizaste en este producto se perderán.',
            action: 'Salir sin guardar',
          },
        })
        .afterClosed(),
    ));
  }

  async init(): Promise<void> {
    this.loading.set(true);
    this.errorInit.set('');
    if (this.isEdit() && !this.productId) {
      this.errorInit.set('El identificador del producto no es válido.');
      this.loading.set(false);
      return;
    }
    try {
      const [cat, prov] = await Promise.all([this.srv.getCategorias(), this.srv.getProveedores()]);
      this.categorias.set(cat);
      this.proveedores.set(prov);

      if (this.productId) {
        const p = await this.srv.get(this.productId);
        this.form.patchValue({
          sku: p.sku,
          nomProducto: p.nomProducto,
          idCategoria: p.idCategoria,
          idProveedor: p.idProveedor || null,
          precio: p.precio,
          stockMinimo: p.stockMinimo,
        });
      }
      this.form.markAsPristine();
    } catch (err) {
      this.errorInit.set(err instanceof Error ? err.message : 'No se pudo cargar el producto.');
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
      const product = await this.srv.save(
        {
          ...value,
          sku: value.sku.trim(),
          nomProducto: value.nomProducto.trim(),
        },
        this.productId,
      );
      this.form.markAsPristine();
      this.notice.show(
        `${product.nomProducto}: ${this.isEdit() ? 'cambios guardados' : 'producto creado correctamente'}.`,
      );
      await this.router.navigate(['/productos']);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Error de validación o conflicto.');
    } finally {
      this.saving.set(false);
    }
  }
}
