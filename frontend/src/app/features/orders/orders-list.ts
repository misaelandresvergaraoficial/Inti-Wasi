import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import {
  enRangoDeEmision,
  ordenarPorEmision,
  OrdenEmision,
} from '../../core/emission-filter';
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
  selector: 'iw-orders-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './orders-list.html',
})
export class OrdersList {
  private readonly orders = inject(OrdersService);
  private readonly dialog = inject(MatDialog);

  readonly auth = inject(AuthService);
  readonly notice = inject(NoticeService);

  readonly records = signal<OrdenResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busyId = signal<number | null>(null);

  readonly query = signal('');
  readonly status = signal('');
  readonly desde = signal('');
  readonly hasta = signal('');
  readonly sort = signal<OrdenEmision>('desc');

  readonly page = signal(0);
  readonly pageSize = 8;

  readonly fecha = fechaVisible;
  readonly importe = importeVisible;

  readonly dateError = computed(() =>
    this.desde() && this.hasta() && this.desde() > this.hasta()
      ? 'La fecha inicial no puede ser posterior a la fecha final.'
      : '',
  );

  readonly hasFilters = computed(
    () =>
      this.query() !== '' ||
      this.status() !== '' ||
      this.desde() !== '' ||
      this.hasta() !== '' ||
      this.sort() !== 'desc',
  );

  readonly filtered = computed(() => {
    if (this.dateError()) return [];

    const query = this.query().trim().toLocaleLowerCase();

    const records = this.records().filter(
      (order) =>
        (!this.status() || order.estado === this.status()) &&
        enRangoDeEmision(order.fechaEmision, this.desde(), this.hasta()) &&
        `${order.idOrden} ${order.nomProveedor}`
          .toLocaleLowerCase()
          .includes(query),
    );

    return ordenarPorEmision(records, this.sort(), (order) => order.idOrden);
  });

  readonly visible = computed(() =>
    this.filtered().slice(
      this.page() * this.pageSize,
      (this.page() + 1) * this.pageSize,
    ),
  );

  readonly pages = computed(() =>
    Math.max(1, Math.ceil(this.filtered().length / this.pageSize)),
  );

  readonly rangeEnd = computed(() =>
    Math.min((this.page() + 1) * this.pageSize, this.filtered().length),
  );

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');

    try {
      this.records.set(await this.orders.list());

      if (this.page() >= this.pages()) {
        this.page.set(this.pages() - 1);
      }
    } catch (error) {
      this.error.set(
        error instanceof Error
          ? error.message
          : 'No se pudieron cargar las órdenes.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  search(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  filterStatus(value: string): void {
    this.status.set(value);
    this.page.set(0);
  }

  changeDate(field: 'desde' | 'hasta', value: string): void {
    (field === 'desde' ? this.desde : this.hasta).set(value);
    this.page.set(0);
  }

  changeSort(value: string): void {
    this.sort.set(value === 'asc' ? 'asc' : 'desc');
    this.page.set(0);
  }

  reset(): void {
    this.query.set('');
    this.status.set('');
    this.desde.set('');
    this.hasta.set('');
    this.sort.set('desc');
    this.page.set(0);
  }

  canEdit(order: OrdenResponse): boolean {
    return puedeEditarOrden(order);
  }

  canCancel(order: OrdenResponse): boolean {
    return puedeCancelarOrden(order);
  }

  receivedUnits(order: OrdenResponse): number {
    return order.detalles.reduce(
      (total, item) => total + item.cantidadRecibida,
      0,
    );
  }

  requestedUnits(order: OrdenResponse): number {
    return order.detalles.reduce(
      (total, item) => total + item.cantidad,
      0,
    );
  }

  async cancel(order: OrdenResponse): Promise<void> {
    if (
      !this.auth.isAdmin() ||
      !this.canCancel(order) ||
      this.busyId() !== null
    ) {
      return;
    }

    const received = order.detalles.some(
      (item) => item.cantidadRecibida > 0,
    );

    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Cancelar la orden N.° ${order.idOrden}?`,
            message:
              'La orden quedará Cancelada y no admitirá nuevas recepciones.' +
              (received
                ? ' Las cantidades ya recibidas y su historial se conservarán.'
                : ''),
            action: 'Cancelar orden',
            danger: true,
          },
        })
        .afterClosed(),
    );

    if (!confirmed) return;

    this.busyId.set(order.idOrden);
    this.error.set('');
    this.notice.clear();

    try {
      await this.orders.cancel(order.idOrden);
      await this.load();

      this.notice.show(
        `La orden N.° ${order.idOrden} quedó Cancelada.`,
      );

      requestAnimationFrame(() =>
        document
          .querySelector<HTMLElement>(
            `[data-order-link="${order.idOrden}"]`,
          )
          ?.focus(),
      );
    } catch (error) {
      this.error.set(
        error instanceof Error
          ? error.message
          : 'No se pudo cancelar la orden.',
      );
    } finally {
      this.busyId.set(null);
    }
  }
}