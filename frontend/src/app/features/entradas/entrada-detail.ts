import { ConfirmDialog, ConfirmData } from '../../shared/confirm-dialog';
import { tieneEmojis } from '../../shared/form-errors';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { EntradaResponse, estadoDocumento, fechaEntrada } from '../../core/entrada-models';
import { EntradasService } from '../../core/entradas.service';
import { NoticeService } from '../../core/notice.service';
import { OrdenResponse } from '../../core/order-models';
import { OrdersService } from '../../core/orders.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-entrada-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './entrada-detail.html',
})
export class EntradaDetail {
  readonly auth = inject(AuthService);
  private readonly entradas = inject(EntradasService);
  private readonly orders = inject(OrdersService);
  private readonly dialog = inject(MatDialog);
  readonly notice = inject(NoticeService);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly record = signal<EntradaResponse | null>(null);
  readonly order = signal<OrdenResponse | null>(null);
  readonly orderError = signal('');
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly motivo = signal('');
  readonly estado = estadoDocumento;

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    this.orderError.set('');
    try {
      if (!Number.isInteger(this.id) || this.id < 1)
        throw new Error('El número de entrada no es válido.');
      const entry = await this.entradas.get(this.id);
      this.record.set(entry);
      try {
        this.order.set(await this.orders.get(entry.idOrden));
      } catch {
        this.order.set(null);
        this.orderError.set('No se pudo consultar el saldo actual de la orden.');
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar la entrada.');
    } finally {
      this.loading.set(false);
    }
  }

  readonly date = fechaEntrada;

  orderLine(idProducto: number) {
    return this.order()?.detalles.find((line) => line.idProducto === idProducto);
  }

  async annul(): Promise<void> {
    if (this.busy() || this.record()?.estadoDocumento !== 1) return;
    const reason = this.motivo().trim();
    if (tieneEmojis(reason)) {
      this.actionError.set('No uses emojis en el motivo de anulación.');
      document.getElementById('motivo-anulacion')?.focus();
      return;
    }
    this.actionError.set('');
    if (!reason || reason.length > 255) {
      this.actionError.set('Escribe un motivo de anulación de hasta 255 caracteres.');
      document.getElementById('motivo-anulacion')?.focus();
      return;
    }
    const entry = this.record();
    if (!entry) return;
    this.busy.set(true);
    try {
      const data: ConfirmData = {
        title: `¿Anular la entrada N.° ${this.id}?`,
        action: 'Anular entrada',
        message: `Orden N.° ${entry.idOrden} · ${entry.numeroGuiaRemision} · Motivo: ${reason}. Se revierte íntegramente la recepción; si falta stock, se rechazará toda la operación.`,
        inventory: entry.movimientos.map((line) => ({
          producto: `${line.sku} · ${line.nomProducto} · ID ${line.idProducto}`,
          cantidad: line.cantidad,
          stockPrevio: line.stockActual,
          cambioNeto: -line.cantidad,
          stockResultado: line.stockActual - line.cantidad,
          reversion: line.cantidad,
        })),
      };
      const confirmed = await firstValueFrom(
        this.dialog
          .open(ConfirmDialog, {
            width: '780px',
            maxWidth: 'calc(100vw - 32px)',
            autoFocus: 'first-tabbable',
            data,
          })
          .afterClosed(),
      );
      if (!confirmed) return;
      await this.entradas.annul(this.id, reason);
      await this.load();
      this.motivo.set('');
      this.notice.show(`Entrada N.° ${this.id} anulada. El historial se conserva.`);
      requestAnimationFrame(() => document.querySelector<HTMLElement>('main h1')?.focus());
    } catch (error) {
      this.actionError.set(
        error instanceof Error ? error.message : 'No se pudo anular la entrada.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
