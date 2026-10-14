import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AjusteRequest, AjusteResponse, TipoAjuste } from '../../core/ajuste-models';
import { AjustesService } from '../../core/ajustes.service';
import { AuthService } from '../../core/auth.service';
import { PendingChanges } from '../../core/guards';
import { productoIdDelError } from '../../core/inventory-error';
import { ApiRequestError } from '../../core/models';
import { NoticeService } from '../../core/notice.service';
import { ProductoResponse } from '../../core/producto-models';
import { ProductosService } from '../../core/productos.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-ajuste-form',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, Icon],
  templateUrl: './ajuste-form.html',
})
export class AjusteForm implements PendingChanges {
  private readonly ajustes = inject(AjustesService);
  private readonly productos = inject(ProductosService);
  private readonly auth = inject(AuthService);
  private readonly notice = inject(NoticeService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly element = viewChild<ElementRef<HTMLFormElement>>('ajusteForm');
  private readonly routeId = inject(ActivatedRoute).snapshot.paramMap.get('id');
  readonly id = this.routeId === null ? undefined : Number(this.routeId);
  readonly form = this.fb.group({
    tipoAjuste: ['', Validators.required],
    motivo: ['', [Validators.required, Validators.maxLength(255)]],
    observaciones: ['', Validators.maxLength(255)],
    motivoCorreccion: ['', Validators.maxLength(255)],
    movimientos: this.fb.array([this.newLine()]),
  });
  readonly products = signal<ProductoResponse[]>([]);
  readonly original = signal<AjusteResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly loadError = signal('');
  readonly error = signal('');
  readonly lineErrors = signal<Record<number, string>>({});
  readonly submitted = signal(false);

  constructor() {
    void this.load();
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
      if (this.id !== undefined && (!Number.isInteger(this.id) || this.id < 1))
        throw new Error('El número de ajuste no es válido.');
      const [products, original] = await Promise.all([
        this.productos.list(),
        this.id === undefined ? Promise.resolve(null) : this.ajustes.get(this.id),
      ]);
      this.products.set(products);
      this.original.set(original);
      if (original) {
        this.form.patchValue({
          tipoAjuste: original.tipoAjuste,
          motivo: original.motivo,
          observaciones: original.observaciones ?? '',
        });
        this.lines.clear();
        for (const item of original.movimientos)
          this.lines.push(this.newLine(item.idProducto, item.cantidad));
      }
      this.form.markAsPristine();
    } catch (error) {
      this.loadError.set(
        error instanceof Error ? error.message : 'No se pudo cargar el formulario.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  stockAfterReversal(id: number): number {
    const current = this.currentStock(id);
    const quantity =
      this.original()?.movimientos.find((item) => item.idProducto === id)?.cantidad ?? 0;
    if (!quantity) return current;
    return current + (this.original()?.tipoAjuste === 'Decremento' ? quantity : -quantity);
  }

  currentStock(id: number): number {
    return this.products().find((item) => item.idProducto === id)?.stockActual ?? 0;
  }

  signedDifference(quantity: number): number {
    return (this.form.controls.tipoAjuste.value === 'Decremento' ? -1 : 1) * quantity;
  }

  resultingStock(id: number, quantity: number): number {
    return this.stockAfterReversal(id) + this.signedDifference(quantity);
  }

  clearLineError(id: number): void {
    this.lineErrors.update((current) => {
      const next = { ...current };
      delete next[id];
      return next;
    });
  }

  reversalPossible(): boolean {
    return (
      this.original()?.movimientos.every((item) => this.stockAfterReversal(item.idProducto) >= 0) ??
      true
    );
  }

  duplicate(index: number): boolean {
    const id = this.lines.at(index).controls.idProducto.value;
    return (
      id > 0 &&
      this.lines.controls.some(
        (line, other) => other !== index && line.controls.idProducto.value === id,
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
    const value = this.form.getRawValue();
    if (
      this.form.invalid ||
      !['Incremento', 'Decremento'].includes(value.tipoAjuste) ||
      !value.motivo.trim() ||
      (this.id !== undefined && !value.motivoCorreccion.trim())
    ) {
      this.error.set('Completa el tipo, el motivo y las cantidades válidas.');
      this.focusInvalid();
      return;
    }
    if (this.id !== undefined && this.original()?.estadoDocumento !== 1) {
      this.error.set('Solo se puede rectificar un ajuste vigente. Actualiza la página.');
      return;
    }
    if (!this.reversalPossible()) {
      this.error.set(
        'El stock actual no alcanza para revertir el incremento original. La rectificación no puede continuar.',
      );
      return;
    }
    if (
      value.movimientos.some(
        (line, index) =>
          this.duplicate(index) ||
          !this.products().some(
            (item) => item.idProducto === line.idProducto && item.estado === 1,
          ) ||
          (value.tipoAjuste === 'Decremento' &&
            line.cantidad > this.stockAfterReversal(line.idProducto)),
      )
    ) {
      this.error.set(
        'Cada producto debe estar activo, aparecer una sola vez y, para Decremento, no superar el stock disponible.',
      );
      this.focusInvalid();
      return;
    }
    const request: AjusteRequest = {
      tipoAjuste: value.tipoAjuste as TipoAjuste,
      motivo: value.motivo.trim(),
      observaciones: value.observaciones.trim() || null,
      movimientos: value.movimientos.map((line) => ({
        idProducto: line.idProducto,
        cantidad: Number(line.cantidad),
      })),
      ...(this.id !== undefined ? { motivoCorreccion: value.motivoCorreccion.trim() } : {}),
    };
    this.busy.set(true);
    try {
      const saved = await this.ajustes.save(request, this.id);
      this.form.markAsPristine();
      this.notice.show(
        `Ajuste N.° ${saved.idAjuste} ${this.id === undefined ? 'registrado' : 'creado como rectificación'} correctamente.`,
      );
      await this.router.navigate(['/ajustes', saved.idAjuste]);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo guardar el ajuste.';
      if (!(error instanceof ApiRequestError) || ![409, 422].includes(error.status)) {
        this.error.set(message);
        return;
      }
      try {
        this.products.set(await this.productos.list());
      } catch {
        this.error.set(`${message} No se pudo actualizar el stock mostrado.`);
        return;
      }
      const lineErrors: Record<number, string> = {};
      for (const line of value.movimientos) {
        if (
          line.idProducto &&
          value.tipoAjuste === 'Decremento' &&
          line.cantidad > this.stockAfterReversal(line.idProducto)
        ) {
          const stock = this.stockAfterReversal(line.idProducto);
          lineErrors[line.idProducto] =
            `Solo se ${stock === 1 ? 'puede descontar' : 'pueden descontar'} ${stock} ${stock === 1 ? 'unidad' : 'unidades'} ahora.`;
        }
      }
      const affectedId = productoIdDelError(error);
      if (
        affectedId &&
        value.movimientos.some((line) => line.idProducto === affectedId) &&
        !lineErrors[affectedId]
      )
        lineErrors[affectedId] = message;
      this.lineErrors.set(lineErrors);
      this.error.set(
        Object.keys(lineErrors).length
          ? 'No se guardó el ajuste. Revisa el producto señalado; el stock se actualizó sin borrar lo escrito.'
          : message,
      );
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
            message: 'Los cambios de este ajuste se perderán.',
            action: 'Salir sin guardar',
          },
        })
        .afterClosed(),
    ));
  }
}
