import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { PendingChanges } from '../../core/guards';
import { NoticeService } from '../../core/notice.service';
import {
  OrdenResponse,
  ProductoOpcion,
  ProveedorOpcion,
  importeVisible,
  puedeEditarOrden,
} from '../../core/order-models';
import { OrdersService } from '../../core/orders.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

function fechaLocal(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

function productsUnique(control: AbstractControl): ValidationErrors | null {
  const ids = (control.value as { idProducto: number }[])
    .map((item) => Number(item.idProducto))
    .filter((id) => id > 0);
  return ids.length === new Set(ids).size ? null : { duplicate: true };
}

@Component({
  selector: 'iw-order-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    Icon,
  ],
  templateUrl: './order-form.html',
})
export class OrderForm implements PendingChanges {
  private readonly orders = inject(OrdersService);
  private readonly auth = inject(AuthService);
  private readonly notice = inject(NoticeService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly element = viewChild<ElementRef<HTMLFormElement>>('orderForm');
  private readonly routeId = inject(ActivatedRoute).snapshot.paramMap.get('id');
  readonly id = this.routeId === null ? undefined : Number(this.routeId);
  readonly minDate = signal(fechaLocal());
  readonly form = this.fb.group({
    idProveedor: [0, [Validators.required, Validators.min(1)]],
    fechaEstimadaEntrega: [
      '',
      (control: AbstractControl) =>
        control.value && control.value < this.minDate() ? { dateBeforeEmission: true } : null,
    ],
    detalles: this.fb.array([this.newLine()], { validators: productsUnique }),
  });
  readonly suppliers = signal<ProveedorOpcion[]>([]);
  readonly products = signal<ProductoOpcion[]>([]);
  readonly original = signal<OrdenResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly loadError = signal('');
  readonly error = signal('');
  readonly submitted = signal(false);
  readonly importe = importeVisible;

  constructor() {
    void this.load();
  }

  get lines() {
    return this.form.controls.detalles;
  }

  private newLine() {
    return this.fb.group({
      idProducto: [0, [Validators.required, Validators.min(1)]],
      cantidad: [
        1,
        [
          Validators.required,
          Validators.min(1),
          Validators.max(2147483647),
          Validators.pattern(/^\d+$/),
        ],
      ],
      precioUnitario: ['', [Validators.required, Validators.pattern(/^\d{1,8}([.,]\d{1,2})?$/)]],
    });
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.loadError.set('');
    try {
      if (this.id !== undefined && (!Number.isInteger(this.id) || this.id < 1)) {
        throw new Error('El número de orden no es válido.');
      }
      const [suppliers, products, order] = await Promise.all([
        this.orders.suppliers(),
        this.orders.products(),
        this.id !== undefined ? this.orders.get(this.id) : Promise.resolve(null),
      ]);
      this.suppliers.set(suppliers);
      this.products.set(products);
      this.original.set(order);
      if (order) {
        this.minDate.set(order.fechaEmision);
        this.form.controls.fechaEstimadaEntrega.updateValueAndValidity();
        this.form.patchValue({
          idProveedor: order.idProveedor,
          fechaEstimadaEntrega: order.fechaEstimadaEntrega ?? '',
        });
        this.lines.clear();
        for (const item of order.detalles) {
          this.lines.push(
            this.fb.group({
              idProducto: [item.idProducto, [Validators.required, Validators.min(1)]],
              cantidad: [
                item.cantidad,
                [
                  Validators.required,
                  Validators.min(1),
                  Validators.max(2147483647),
                  Validators.pattern(/^\d+$/),
                ],
              ],
              precioUnitario: [
                String(item.precioUnitario),
                [Validators.required, Validators.pattern(/^\d{1,8}([.,]\d{1,2})?$/)],
              ],
            }),
          );
        }
        this.form.markAsPristine();
      }
    } catch (error) {
      this.loadError.set(
        error instanceof Error ? error.message : 'No se pudieron cargar los datos.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  canEdit(order: OrdenResponse): boolean {
    return puedeEditarOrden(order);
  }

  addLine(): void {
    this.lines.push(this.newLine());
    this.form.markAsDirty();
  }

  removeLine(index: number): void {
    if (this.lines.length < 2) return;
    this.lines.removeAt(index);
    this.form.markAsDirty();
  }

  duplicate(index: number): boolean {
    const id = Number(this.lines.at(index).controls.idProducto.value);
    return (
      id > 0 &&
      this.lines.controls.some(
        (line, other) => other !== index && Number(line.controls.idProducto.value) === id,
      )
    );
  }

  hasSupplierOption(id: number): boolean {
    return this.suppliers().some((item) => item.idProveedor === id);
  }

  missingProduct(id: number) {
    return this.products().some((item) => item.idProducto === id)
      ? undefined
      : this.original()?.detalles.find((item) => item.idProducto === id);
  }

  productsForSupplier(): ProductoOpcion[] {
    const supplierId = this.form.controls.idProveedor.value;
    return this.products().filter((product) => product.idProveedor === supplierId);
  }

  productMismatch(id: number): boolean {
    const product = this.products().find((item) => item.idProducto === Number(id));
    return !!product && product.idProveedor !== this.form.controls.idProveedor.value;
  }

  mismatchedProduct(id: number): ProductoOpcion | undefined {
    return this.products().find((item) => item.idProducto === Number(id));
  }

  onProductChange(index: number): void {
    const line = this.lines.at(index);
    const product = this.productsForSupplier().find(
      (item) => item.idProducto === Number(line.controls.idProducto.value),
    );
    line.controls.precioUnitario.setValue(product ? product.precio.toFixed(2) : '');
  }

  lineSubtotal(index: number): number {
    const line = this.lines.at(index);
    const quantity = Number(line.controls.cantidad.value);
    const price = Number(line.controls.precioUnitario.value.replace(',', '.'));
    return Number.isFinite(quantity * price) ? quantity * price : 0;
  }

  previewTotal(): number {
    return this.lines.controls.reduce((total, _, index) => total + this.lineSubtotal(index), 0);
  }

  async submit(): Promise<void> {
    if (this.busy()) return;
    this.submitted.set(true);
    this.error.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.lines.length) {
      requestAnimationFrame(() =>
        this.element()
          ?.nativeElement.querySelector<HTMLElement>(
            'input.ng-invalid, select.ng-invalid, select[aria-invalid="true"]',
          )
          ?.focus(),
      );
      return;
    }
    const value = this.form.getRawValue();
    const supplierAvailable = this.suppliers().some(
      (item) => item.idProveedor === value.idProveedor,
    );
    const unavailableProduct = value.detalles.findIndex(
      (line) =>
        !this.products().some(
          (item) => item.idProducto === line.idProducto && item.idProveedor === value.idProveedor,
        ),
    );
    if (!supplierAvailable || unavailableProduct >= 0) {
      this.error.set('Selecciona un proveedor activo y productos asignados a ese proveedor.');
      requestAnimationFrame(() =>
        this.element()
          ?.nativeElement.querySelector<HTMLElement>(
            supplierAvailable ? `#line-product-${unavailableProduct}` : '#order-supplier',
          )
          ?.focus(),
      );
      return;
    }
    this.busy.set(true);
    try {
      const saved = await this.orders.save(
        {
          idProveedor: value.idProveedor,
          fechaEstimadaEntrega: value.fechaEstimadaEntrega || null,
          detalles: value.detalles.map((line) => ({
            idProducto: line.idProducto,
            cantidad: Number(line.cantidad),
            precioUnitario: Number(line.precioUnitario.replace(',', '.')),
          })),
        },
        this.id,
      );
      this.form.markAsPristine();
      this.notice.show(
        `Orden N.° ${saved.idOrden} ${this.id ? 'actualizada' : 'creada'} correctamente.`,
      );
      await this.router.navigate(['/ordenes-compra', saved.idOrden]);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo guardar la orden.');
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
            message: 'Los cambios que realizaste en esta orden se perderán.',
            action: 'Salir sin guardar',
          },
        })
        .afterClosed(),
    ));
  }
}
