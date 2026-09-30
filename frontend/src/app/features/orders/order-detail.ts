import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import {
  OrdenResponse,
  fechaVisible,
  importeVisible,
  puedeCancelarOrden,
  puedeEditarOrden,
} from '../../core/order-models';
import { OrdersService } from '../../core/orders.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-order-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './order-detail.html',
})
export class OrderDetail {
  private readonly orders = inject(OrdersService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);
  readonly notice = inject(NoticeService);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly record = signal<OrdenResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly fecha = fechaVisible;
  readonly importe = importeVisible;

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      if (!Number.isInteger(this.id) || this.id < 1)
        throw new Error('El número de orden no es válido.');
      this.record.set(await this.orders.get(this.id));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar la orden.');
    } finally {
      this.loading.set(false);
    }
  }

  canEdit(order: OrdenResponse): boolean {
    return puedeEditarOrden(order);
  }

  canCancel(order: OrdenResponse): boolean {
    return puedeCancelarOrden(order);
  }

  async cancel(order: OrdenResponse): Promise<void> {
    if (!this.auth.isAdmin() || !this.canCancel(order) || this.busy()) return;
    const received = order.detalles.some((item) => item.cantidadRecibida > 0);
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Cancelar la orden N.° ${order.idOrden}?`,
            message: `La orden quedará Cancelada y no admitirá nuevas recepciones.${received ? ' Las cantidades ya recibidas y su historial se conservarán.' : ''}`,
            action: 'Cancelar orden',
            danger: true,
          },
        })
        .afterClosed(),
    );
    if (!confirmed) return;
    this.busy.set(true);
    this.actionError.set('');
    this.notice.clear();
    try {
      await this.orders.cancel(order.idOrden);
      await this.load();
      this.notice.show(`La orden N.° ${order.idOrden} quedó Cancelada.`);
      requestAnimationFrame(() => document.querySelector<HTMLElement>('main h1')?.focus());
    } catch (error) {
      this.actionError.set(
        error instanceof Error ? error.message : 'No se pudo cancelar la orden.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
