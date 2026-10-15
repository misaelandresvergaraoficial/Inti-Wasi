import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { EntradaRequest, EntradaResponse } from '../../core/entrada-models';
import { ApiRequestError } from '../../core/models';
import { EntradasService } from '../../core/entradas.service';
import { PendingChanges } from '../../core/guards';
import { productoIdDelError } from '../../core/inventory-error';
import { NoticeService } from '../../core/notice.service';
import { OrdenResponse } from '../../core/order-models';
import { OrdersService } from '../../core/orders.service';
import { ProductoResponse } from '../../core/producto-models';
import { ProductosService } from '../../core/productos.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-entrada-form',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, Icon],
  templateUrl: './entrada-form.html',
})
export class EntradaForm implements PendingChanges {
  private readonly entradas = inject(EntradasService);
  private readonly orders = inject(OrdersService);
  private readonly productos = inject(ProductosService);
  private readonly auth = inject(AuthService);
  private readonly notice = inject(NoticeService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly element =
    viewChild<ElementRef<HTMLFormElement>>('entradaForm');
  private readonly route = inject(ActivatedRoute);

  readonly id = this.route.snapshot.paramMap.has('id')
    ? Number(this.route.snapshot.paramMap.get('id'))
    : undefined;

  readonly form = this.fb.group({
    idOrden: [0, [Validators.required, Validators.min(1)]],
    numeroGuiaRemision: ['', [Validators.required, Validators.maxLength(50)]],
    observaciones: ['', Validators.maxLength(255)],
    motivoCorreccion: ['', Validators.maxLength(255)],
    movimientos: this.fb.array([this.newLine()]),
  });

  readonly ordersList = signal<OrdenResponse[]>([]);
  readonly order = signal<OrdenResponse | null>(null);
  readonly products = signal<ProductoResponse[]>([]);
  readonly original = signal<EntradaResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly loadError = signal('');
  readonly error = signal('');
  readonly lineErrors = signal<Record<number, string>>({});
  readonly submitted = signal(false);

  private orderRequestId = 0;

  constructor() {
    this.form.controls.numeroGuiaRemision.addValidators((control) => {
      const message = this.validarGuia(control.value);
      return message ? { guia: message } : null;
    });

    void this.load();
  }

  private validarGuia(value: string): string {
    if (this.id !== undefined) return '';

    const selected = this.order();
    if (!selected) return '';

    const prefix = selected.prefijoGuia;

    if (!prefix || !/^GUIA-[A-Z][A-Z0-9]{0,23}-$/.test(prefix)) {
      return 'El proveedor no tiene un prefijo válido. Solicita al Administrador que lo configure.';
    }

    const number = (value ?? '').trim();

    if (!number) {
      return 'Ingresa el número de guía de remisión.';
    }

    if (!number.startsWith(prefix)) {
      return `La guía no corresponde al proveedor de la orden. Para ${selected.nomProveedor} debe comenzar con ${prefix}`;
    }

    if (!/^[0-9]+(?:-[0-9]+)*$/.test(number.slice(prefix.length))) {
      return `Después de ${prefix} ingresa dígitos, opcionalmente separados por guiones; por ejemplo, 001-000123.`;
    }

    return '';
  }

  guiaError(): string {
    const control = this.form.controls.numeroGuiaRemision;

    if (control.hasError('server')) return control.getError('server');
    if (control.hasError('guia')) return control.getError('guia');

    if (control.hasError('maxlength')) {
      return 'La guía completa no debe exceder 50 caracteres.';
    }

    if (control.invalid) {
      return 'Ingresa el número de guía de remisión.';
    }

    return '';
  }

  mostrarErrorGuia(): boolean {
    const control = this.form.controls.numeroGuiaRemision;

    return (
      control.invalid &&
      (control.dirty || control.touched || this.submitted())
    );
  }

  get lines() {
    return this.form.controls.movimientos;
  }

  private newLine(idProducto = 0, cantidad = 1) {
    return this.fb.group({
      idProducto: [idProducto, [Validators.required, Validators.min(1)]],
      cantidad: [
        cantidad,
        [
          Validators.required,
          Validators.min(1),
          Validators.max(2147483647),
          Validators.pattern(/^\d+$/),
        ],
      ],
    });
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.loadError.set('');

    try {
      if (
        this.id !== undefined &&
        (!Number.isInteger(this.id) || this.id < 1)
      ) {
        throw new Error('El número de entrada no es válido.');
      }

      const [orders, products, original] = await Promise.all([
        this.orders.list(),
        this.productos.list(),
        this.id === undefined
          ? Promise.resolve(null)
          : this.entradas.get(this.id),
      ]);

      this.ordersList.set(orders);
      this.products.set(products);
      this.original.set(original);

      const selectedId =
        original?.idOrden ??
        Number(this.route.snapshot.queryParamMap.get('orden'));

      if (selectedId > 0) {
        this.form.controls.idOrden.setValue(selectedId);
        await this.selectOrder(selectedId);
      }

      if (original) {
        this.form.patchValue({
          numeroGuiaRemision: original.numeroGuiaRemision,
          observaciones: original.observaciones ?? '',
        });

        this.lines.clear();

        for (const item of original.movimientos) {
          this.lines.push(this.newLine(item.idProducto, item.cantidad));
        }
      }

      this.form.markAsPristine();
    } catch (error) {
      this.loadError.set(
        error instanceof Error
          ? error.message
          : 'No se pudo cargar el formulario.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  async selectOrder(id: number): Promise<void> {
    const requestId = ++this.orderRequestId;

    this.order.set(null);
    this.error.set('');
    this.form.controls.numeroGuiaRemision.updateValueAndValidity();

    if (!id) return;

    try {
      const selected = await this.orders.get(id);

      if (requestId !== this.orderRequestId) return;

      this.order.set(selected);
      this.form.controls.numeroGuiaRemision.updateValueAndValidity();

      if (
        this.id === undefined &&
        this.lines.length === 1 &&
        !this.lines.at(0).controls.idProducto.value
      ) {
        const available = this.availableProducts();

        if (available.length === 1) {
          this.lines.at(0).controls.idProducto.setValue(available[0].idProducto);
        }
      }
    } catch (error) {
      if (requestId === this.orderRequestId) {
        this.error.set(
          error instanceof Error
            ? error.message
            : 'No se pudo cargar la orden.',
        );
      }
    }
  }

  onOrderChange(): void {
    const id = this.form.controls.idOrden.value;

    this.lines.clear();
    this.lines.push(this.newLine());

    void this.selectOrder(id);
  }

  availableOrders(): OrdenResponse[] {
    return this.ordersList().filter(
      (order) => order.estado === 'Pendiente' || order.estado === 'Parcial',
    );
  }

  availableProducts() {
    return (
      this.order()?.detalles.filter(
        (item) =>
          this.balance(item.idProducto) > 0 &&
          this.products().some(
            (product) =>
              product.idProducto === item.idProducto && product.estado === 1,
          ),
      ) ?? []
    );
  }

  balance(id: number): number {
    const detail = this.orderLine(id);
    if (!detail) return 0;

    const previouslyReceived =
      this.original()?.movimientos.find((item) => item.idProducto === id)
        ?.cantidad ?? 0;

    return this.order()?.estado === 'Cancelada'
      ? previouslyReceived
      : detail.cantidadPorRecibir + previouslyReceived;
  }

  orderLine(id: number) {
    return this.order()?.detalles.find((item) => item.idProducto === id);
  }

  originalQuantity(id: number): number {
    return (
      this.original()?.movimientos.find((item) => item.idProducto === id)
        ?.cantidad ?? 0
    );
  }

  clearLineError(id: number): void {
    this.lineErrors.update((current) => {
      const next = { ...current };
      delete next[id];
      return next;
    });
  }

  duplicate(index: number): boolean {
    const id = this.lines.at(index).controls.idProducto.value;

    return (
      id > 0 &&
      this.lines.controls.some(
        (line, other) =>
          other !== index && line.controls.idProducto.value === id,
      )
    );
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

  async submit(): Promise<void> {
    if (this.busy()) return;

    this.submitted.set(true);
    this.error.set('');
    this.lineErrors.set({});
    this.form.markAllAsTouched();
    this.form.controls.numeroGuiaRemision.updateValueAndValidity();

    const value = this.form.getRawValue();

    if (
      this.form.invalid ||
      !value.numeroGuiaRemision.trim() ||
      (this.id !== undefined && !value.motivoCorreccion.trim())
    ) {
      this.error.set(
        this.guiaError() ||
          'Completa los campos obligatorios y revisa las cantidades.',
      );
      this.focusInvalid();
      return;
    }

    const order = this.order();

    if (
      !order ||
      order.idOrden !== value.idOrden ||
      (this.id === undefined &&
        !['Pendiente', 'Parcial'].includes(order.estado)) ||
      (this.id !== undefined && this.original()?.estadoDocumento !== 1)
    ) {
      this.error.set(
        'La entrada o la orden ya no admite esta operación. Actualiza la página.',
      );
      return;
    }

    if (
      value.movimientos.some(
        (line, index) =>
          this.duplicate(index) ||
          !this.availableProducts().some(
            (item) => item.idProducto === line.idProducto,
          ) ||
          line.cantidad > this.balance(line.idProducto),
      )
    ) {
      this.error.set(
        'Cada producto debe estar activo, aparecer una sola vez y no superar el saldo permitido de la orden.',
      );
      this.focusInvalid();
      return;
    }

    const request: EntradaRequest = {
      idOrden: value.idOrden,
      numeroGuiaRemision: value.numeroGuiaRemision.trim(),
      observaciones: value.observaciones.trim() || null,
      movimientos: value.movimientos.map((line) => ({
        idProducto: line.idProducto,
        cantidad: Number(line.cantidad),
      })),
      ...(this.id !== undefined
        ? { motivoCorreccion: value.motivoCorreccion.trim() }
        : {}),
    };

    this.busy.set(true);

    try {
      const saved = await this.entradas.save(request, this.id);

      this.form.markAsPristine();

      this.notice.showNext(
        `Entrada N.° ${saved.idEntrada} ${
          this.id === undefined
            ? 'registrada'
            : 'creada como rectificación'
        } correctamente.`,
      );

      await this.router.navigate(['/entradas', saved.idEntrada]);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : 'No se pudo guardar la entrada.';

      if (
        error instanceof ApiRequestError &&
        error.field === 'numeroGuiaRemision'
      ) {
        const control = this.form.controls.numeroGuiaRemision;

        control.setErrors({
          ...control.errors,
          server: message,
        });
        control.markAsTouched();

        this.error.set(message);
        this.focusInvalid();
        return;
      }

      if (
        !(error instanceof ApiRequestError) ||
        ![409, 422].includes(error.status)
      ) {
        this.error.set(message);
        return;
      }

      const [updatedOrder, updatedProducts] = await Promise.allSettled([
        this.orders.get(value.idOrden),
        this.productos.list(),
      ]);

      if (updatedOrder.status === 'fulfilled') {
        this.order.set(updatedOrder.value);
      }

      if (updatedProducts.status === 'fulfilled') {
        this.products.set(updatedProducts.value);
      }

      const lineErrors: Record<number, string> = {};

      if (updatedOrder.status === 'fulfilled') {
        for (const line of value.movimientos) {
          if (
            line.idProducto &&
            line.cantidad > this.balance(line.idProducto)
          ) {
            lineErrors[line.idProducto] =
              `El máximo disponible ahora es ${this.balance(line.idProducto)}.`;
          }
        }
      }

      const affectedId = productoIdDelError(error);

      if (
        affectedId &&
        value.movimientos.some(
          (line) => line.idProducto === affectedId,
        ) &&
        !lineErrors[affectedId]
      ) {
        lineErrors[affectedId] = message;
      }

      this.lineErrors.set(lineErrors);

      const refreshed =
        updatedOrder.status === 'fulfilled' &&
        updatedProducts.status === 'fulfilled';

      if (!refreshed) {
        this.error.set(
          `${message} No se pudieron actualizar todos los saldos mostrados. Los datos escritos se conservaron.`,
        );
      } else {
        this.error.set(
          Object.keys(lineErrors).length
            ? 'No se guardó la entrada. Revisa el producto señalado; los saldos se actualizaron sin borrar lo escrito.'
            : message,
        );
      }
    } finally {
      this.busy.set(false);
    }
  }

  private focusInvalid(): void {
    requestAnimationFrame(() =>
      this.element()
        ?.nativeElement.querySelector<HTMLElement>(
          'input.ng-invalid, select.ng-invalid, input[aria-invalid="true"], select[aria-invalid="true"]',
        )
        ?.focus(),
    );
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
            message: 'Los cambios de esta entrada se perderán.',
            action: 'Salir sin guardar',
          },
        })
        .afterClosed(),
    ));
  }
}