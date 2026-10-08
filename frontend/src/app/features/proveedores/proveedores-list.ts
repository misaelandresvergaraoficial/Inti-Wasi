import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { ProveedorResponse } from '../../core/proveedor-models';
import { ProveedoresService } from '../../core/proveedores.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-proveedores-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './proveedores-list.html',
})
export class ProveedoresList {
  private readonly suppliers = inject(ProveedoresService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);
  readonly notice = inject(NoticeService);

  readonly records = signal<ProveedorResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busyId = signal<number | null>(null);
  readonly query = signal('');
  readonly statusFilter = signal('');
  readonly page = signal(0);
  readonly pageSize = 10;

  readonly filtered = computed(() => {
    const term = this.query().trim().toLocaleLowerCase();
    return this.records().filter((supplier) => {
      const matchesStatus =
        !this.statusFilter() || supplier.estado === Number(this.statusFilter());
      const matchesTerm =
        !term ||
        [
          supplier.nomProveedor,
          supplier.ruc,
          supplier.contacto,
          supplier.telefono,
          supplier.direccion,
        ]
          .filter((value): value is string => !!value)
          .some((value) => value.toLocaleLowerCase().includes(term));
      return matchesStatus && matchesTerm;
    });
  });
  readonly visible = computed(() =>
    this.filtered().slice(this.page() * this.pageSize, (this.page() + 1) * this.pageSize),
  );
  readonly pages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize)));
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
      this.records.set(await this.suppliers.list(this.auth.isAdmin()));
      if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'No se pudieron cargar los proveedores.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  search(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  reset(): void {
    this.query.set('');
    this.statusFilter.set('');
    this.page.set(0);
  }

  filterStatus(value: string): void {
    this.statusFilter.set(value);
    this.page.set(0);
  }

  async deactivate(supplier: ProveedorResponse): Promise<void> {
    if (this.busyId() !== null) return;
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Desactivar a ${supplier.nomProveedor}?`,
            message:
              'El proveedor dejará de aparecer en las selecciones para nuevos productos y órdenes. Su información y las órdenes existentes se conservarán.',
            action: 'Desactivar proveedor',
            danger: true,
          },
        })
        .afterClosed(),
    );
    if (!confirmed) return;

    this.busyId.set(supplier.idProveedor);
    this.error.set('');
    this.notice.clear();
    try {
      await this.suppliers.deactivate(supplier.idProveedor);
      this.records.update((records) =>
        records.map((record) =>
          record.idProveedor === supplier.idProveedor ? { ...record, estado: 0 } : record,
        ),
      );
      this.afterStateChange(supplier.idProveedor);
      this.notice.show(
        `${supplier.nomProveedor} fue desactivado. Puedes reactivarlo desde este listado.`,
      );
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'No se pudo desactivar al proveedor.',
      );
    } finally {
      this.busyId.set(null);
    }
  }

  async activate(supplier: ProveedorResponse): Promise<void> {
    if (this.busyId() !== null) return;
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Reactivar a ${supplier.nomProveedor}?`,
            message:
              'El proveedor volverá a estar disponible para nuevos productos y órdenes de compra.',
            action: 'Reactivar proveedor',
          },
        })
        .afterClosed(),
    );
    if (!confirmed) return;

    this.busyId.set(supplier.idProveedor);
    this.error.set('');
    this.notice.clear();
    try {
      await this.suppliers.activate(supplier.idProveedor);
      this.records.update((records) =>
        records.map((record) =>
          record.idProveedor === supplier.idProveedor ? { ...record, estado: 1 } : record,
        ),
      );
      this.afterStateChange(supplier.idProveedor);
      this.notice.show(`${supplier.nomProveedor} fue reactivado.`);
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'No se pudo reactivar al proveedor.',
      );
    } finally {
      this.busyId.set(null);
    }
  }

  private afterStateChange(id: number): void {
    if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    requestAnimationFrame(() => {
      (
        document.querySelector<HTMLElement>(`[data-supplier-action="${id}"]`) ??
        document.getElementById('supplier-search')
      )?.focus();
    });
  }
}
