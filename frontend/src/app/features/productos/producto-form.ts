import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ProductosService } from '../../core/productos.service';
import { NoticeService } from '../../core/notice.service';
import { PendingChanges } from '../../core/guards';
import { CategoriaResponse, ProveedorResponse } from '../../core/producto-models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-producto-form',
  imports: [
    ReactiveFormsModule, RouterLink, MatButtonModule, 
    MatFormFieldModule, MatInputModule, MatSelectModule, Icon
  ],
  templateUrl: './producto-form.html'
})
export class ProductoForm implements PendingChanges {
  @ViewChild('firstInput') firstInput!: ElementRef<HTMLInputElement>;
  
  private readonly fb = inject(FormBuilder);
  private readonly srv = inject(ProductosService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);

  readonly isEdit = signal(false);
  readonly saving = signal(false);
  readonly errorInit = signal('');
  readonly error = signal('');
  
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly proveedores = signal<ProveedorResponse[]>([]);
  
  private productId?: number;

  readonly form = this.fb.nonNullable.group({
    sku: ['', [Validators.required, Validators.maxLength(30)]],
    nomProducto: ['', [Validators.required, Validators.maxLength(150)]],
    idCategoria: [0, [Validators.required, Validators.min(1)]],
    idProveedor: [null as number | null],
    precio: [0, [Validators.required, Validators.min(0)]],
    stockMinimo: [0, [Validators.required, Validators.min(0)]]
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      this.productId = Number(id);
    }
    void this.init();
  }

  canLeave(): boolean {
    return !this.form.dirty || this.saving();
  }

  async init(): Promise<void> {
    try {
      const [cat, prov] = await Promise.all([
        this.srv.getCategorias(),
        this.srv.getProveedores()
      ]);
      this.categorias.set(cat);
      this.proveedores.set(prov);

      if (this.isEdit() && this.productId) {
        const p = await this.srv.get(this.productId);
        this.form.patchValue({
          sku: p.sku,
          nomProducto: p.nomProducto,
          idCategoria: p.idCategoria,
          idProveedor: p.idProveedor || null,
          precio: p.precio,
          stockMinimo: p.stockMinimo
        });
      }
      setTimeout(() => this.firstInput?.nativeElement?.focus(), 100);
    } catch (err) {
      this.errorInit.set('Error al cargar datos necesarios. Verifica tu conexión.');
    }
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set('');
    try {
      await this.srv.save(this.form.getRawValue(), this.productId);
      this.form.markAsPristine(); 
      this.notice.show(`Producto ${this.isEdit() ? 'actualizado' : 'creado'} exitosamente.`);
      void this.router.navigate(['/productos']);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Error de validación o conflicto.');
    } finally {
      this.saving.set(false);
    }
  }
}