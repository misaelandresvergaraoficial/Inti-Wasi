import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { PendingChanges } from '../../core/guards';
import { NoticeService } from '../../core/notice.service';
import { ProductoResponse } from '../../core/producto-models';
import { ProductosService } from '../../core/productos.service';
import { MotivoSalida, SalidaRequest, SalidaResponse } from '../../core/salida-models';
import { SalidasService } from '../../core/salidas.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

const motivos: MotivoSalida[] = ['Despacho/Venta', 'Merma', 'Otro'];

@Component({
  selector: 'iw-salida-form',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, Icon],
  templateUrl: './salida-form.html',
})
export class SalidaForm implements PendingChanges {
  private readonly salidas = inject(SalidasService);
  private readonly productos = inject(ProductosService);
  private readonly auth = inject(AuthService);
  private readonly notice = inject(NoticeService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly element = viewChild<ElementRef<HTMLFormElement>>('salidaForm');
  private readonly routeId = inject(ActivatedRoute).snapshot.paramMap.get('id');
  readonly id = this.routeId === null ? undefined : Number(this.routeId);
  readonly form = this.fb.group({
    motivo: ['', Validators.required],
    observaciones: ['', Validators.maxLength(255)],
    motivoCorreccion: ['', Validators.maxLength(255)],
    movimientos: this.fb.array([this.newLine()]),
  });
  readonly products = signal<ProductoResponse[]>([]);
  readonly original = signal<SalidaResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly loadError = signal('');
  readonly error = signal('');
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
        throw new Error('El número de salida no es válido.');
      const [products, original] = await Promise.all([
        this.productos.list(),
        this.id === undefined ? Promise.resolve(null) : this.salidas.get(this.id),
      ]);
      this.products.set(products);
      this.original.set(original);
      if (original) {
        this.form.patchValue({
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

  availableStock(id: number): number {
    const current = this.products().find((product) => product.idProducto === id)?.stockActual ?? 0;
    const returned =
      this.original()?.movimientos.find((item) => item.idProducto === id)?.cantidad ?? 0;
    return current + returned;
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
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (
      this.form.invalid ||
      !motivos.includes(value.motivo as MotivoSalida) ||
      (this.id !== undefined && !value.motivoCorreccion.trim())
    ) {
      this.error.set('Completa los campos obligatorios y revisa las cantidades.');
      this.focusInvalid();
      return;
    }
    if (this.id !== undefined && this.original()?.estadoDocumento !== 1) {
      this.error.set('Solo se puede rectificar una salida vigente. Actualiza la página.');
      return;
    }
    if (
      value.movimientos.some(
        (line, index) =>
          this.duplicate(index) ||
          !this.products().some(
            (item) => item.idProducto === line.idProducto && item.estado === 1,
          ) ||
          line.cantidad > this.availableStock(line.idProducto),
      )
    ) {
      this.error.set(
        'Cada producto debe estar activo, aparecer una sola vez y tener stock suficiente.',
      );
      this.focusInvalid();
      return;
    }
    const request: SalidaRequest = {
      motivo: value.motivo as MotivoSalida,
      observaciones: value.observaciones.trim() || null,
      movimientos: value.movimientos.map((line) => ({
        idProducto: line.idProducto,
        cantidad: Number(line.cantidad),
      })),
      ...(this.id !== undefined ? { motivoCorreccion: value.motivoCorreccion.trim() } : {}),
    };
    this.busy.set(true);
    try {
      const saved = await this.salidas.save(request, this.id);
      this.form.markAsPristine();
      this.notice.show(
        `Salida N.° ${saved.idSalida} ${this.id === undefined ? 'registrada' : 'creada como rectificación'} correctamente.`,
      );
      await this.router.navigate(['/salidas', saved.idSalida]);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo guardar la salida.');
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
            message: 'Los cambios de esta salida se perderán.',
            action: 'Salir sin guardar',
          },
        })
        .afterClosed(),
    ));
  }
}
