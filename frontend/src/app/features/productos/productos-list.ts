import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { ProductoResponse } from '../../core/producto-models';
import { ProductosService } from '../../core/productos.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

const penCurrency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });

@Component({
  selector: 'iw-productos-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './productos-list.html',
})
export class ProductosList {
  private readonly srv = inject(ProductosService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);
  readonly notice = inject(NoticeService);

  readonly records = signal<ProductoResponse[]>([]);
  readonly categorias = computed(() =>
    [...new Set(this.records().map((product) => product.nomCategoria))].sort((a, b) =>
      a.localeCompare(b, 'es'),
    ),
  );
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busyId = signal<number | null>(null);

  readonly query = signal('');
  readonly categoriaFilter = signal('');
  readonly statusFilter = signal('');
  readonly page = signal(0);
  readonly pageSize = 10;

  readonly filtered = computed(() =>
    this.records().filter((p) => {
      const matchesStatus = this.statusFilter() === '' || p.estado === Number(this.statusFilter());
      const matchesCategoria =
        this.categoriaFilter() === '' || p.nomCategoria === this.categoriaFilter();
      const searchTerm = this.query().trim().toLowerCase();
      const matchesQuery =
        !searchTerm ||
        p.sku.toLowerCase().includes(searchTerm) ||
        p.nomProducto.toLowerCase().includes(searchTerm);

      return matchesStatus && matchesCategoria && matchesQuery;
    }),
  );

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
      this.records.set(await this.srv.list(this.auth.isAdmin()));
      if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Error de servidor.');
    } finally {
      this.loading.set(false);
    }
  }

  search(val: string): void {
    this.query.set(val);
    this.page.set(0);
  }

  filterCategoria(val: string): void {
    this.categoriaFilter.set(val);
    this.page.set(0);
  }

  filterStatus(val: string): void {
    this.statusFilter.set(val);
    this.page.set(0);
  }

  reset(): void {
    this.query.set('');
    this.categoriaFilter.set('');
    this.statusFilter.set('');
    this.page.set(0);
  }

  price(value: number): string {
    return penCurrency.format(value);
  }

  async desactivar(p: ProductoResponse): Promise<void> {
    if (this.busyId() !== null) return;
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Desactivar ${p.nomProducto}?`,
            message:
              'El producto dejará de estar disponible para nuevos movimientos de inventario. Su información e historial se conservarán.',
            action: 'Desactivar producto',
            danger: true,
          },
        })
        .afterClosed(),
    );

    if (!confirmed) return;
    this.busyId.set(p.idProducto);
    this.error.set('');
    this.notice.clear();
    try {
      await this.srv.delete(p.idProducto);
      this.records.update((curr) =>
        curr.map((x) => (x.idProducto === p.idProducto ? { ...x, estado: 0 } : x)),
      );
      this.afterStateChange(p.idProducto);
      this.notice.show(`${p.nomProducto} fue desactivado. Puede reactivarse desde este listado.`);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo desactivar el producto.');
    } finally {
      this.busyId.set(null);
    }
  }

  async reactivar(p: ProductoResponse): Promise<void> {
    if (this.busyId() !== null) return;
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Reactivar ${p.nomProducto}?`,
            message: 'El producto volverá a estar disponible para los movimientos de inventario.',
            action: 'Reactivar producto',
          },
        })
        .afterClosed(),
    );

    if (!confirmed) return;
    this.busyId.set(p.idProducto);
    this.error.set('');
    this.notice.clear();
    try {
      await this.srv.activate(p.idProducto);
      this.records.update((curr) =>
        curr.map((x) => (x.idProducto === p.idProducto ? { ...x, estado: 1 } : x)),
      );
      this.afterStateChange(p.idProducto);
      this.notice.show(`${p.nomProducto} fue reactivado.`);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo reactivar el producto.');
    } finally {
      this.busyId.set(null);
    }
  }

  private afterStateChange(id: number): void {
    if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    requestAnimationFrame(() =>
      (
        document.querySelector<HTMLElement>(`[data-product-state-action="${id}"]`) ??
        document.getElementById('producto-status')
      )?.focus(),
    );
  }
}
