import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { CategoriaResponse } from '../../core/categoria-models';
import { CategoriasService } from '../../core/categorias.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-categorias-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './categorias.html',
})
export class CategoriasList {
  private readonly srv = inject(CategoriasService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);
  readonly notice = inject(NoticeService);

  readonly records = signal<CategoriaResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly busyId = signal<number | null>(null);

  readonly query = signal('');
  readonly statusFilter = signal('1');
  readonly page = signal(0);
  readonly pageSize = 10;

  readonly filtered = computed(() =>
    this.records().filter((c) => {
      const matchesStatus = this.statusFilter() === '' || c.estado === Number(this.statusFilter());
      const searchTerm = this.query().trim().toLowerCase();
      const matchesQuery = !searchTerm || c.nomCategoria.toLowerCase().includes(searchTerm);
      return matchesStatus && matchesQuery;
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
      this.records.set(await this.srv.list(true));
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

  filterStatus(val: string): void {
    this.statusFilter.set(val);
    this.page.set(0);
  }

  reset(): void {
    this.query.set('');
    this.statusFilter.set('');
    this.page.set(0);
  }

  async desactivar(c: CategoriaResponse): Promise<void> {
    if (this.busyId() !== null) return;
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Desactivar ${c.nomCategoria}?`,
            message:
              'También se desactivarán sus productos activos. La categoría y el historial se conservarán. Al reactivarla, deberás reactivar cada producto por separado.',
            action: 'Desactivar categoría',
            danger: true,
          },
        })
        .afterClosed(),
    );

    if (!confirmed) return;
    this.busyId.set(c.idCategoria);
    this.error.set('');
    this.notice.clear();
    try {
      await this.srv.delete(c.idCategoria);
      this.records.update((curr) =>
        curr.map((x) => (x.idCategoria === c.idCategoria ? { ...x, estado: 0 } : x)),
      );
      this.afterStateChange(c.idCategoria);
      this.notice.show(
        `${c.nomCategoria} fue desactivada. Los productos de esta categoría también se desactivaron.`,
      );
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo desactivar la categoría.');
    } finally {
      this.busyId.set(null);
    }
  }

  async reactivar(c: CategoriaResponse): Promise<void> {
    if (this.busyId() !== null) return;
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Reactivar ${c.nomCategoria}?`,
            message:
              'La categoría volverá a estar disponible para asignarse a productos nuevos o existentes.',
            action: 'Reactivar categoría',
          },
        })
        .afterClosed(),
    );

    if (!confirmed) return;
    this.busyId.set(c.idCategoria);
    this.error.set('');
    this.notice.clear();
    try {
      await this.srv.activate(c.idCategoria);
      this.records.update((curr) =>
        curr.map((x) => (x.idCategoria === c.idCategoria ? { ...x, estado: 1 } : x)),
      );
      this.afterStateChange(c.idCategoria);
      this.notice.show(`${c.nomCategoria} fue reactivada.`);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo reactivar la categoría.');
    } finally {
      this.busyId.set(null);
    }
  }

  private afterStateChange(id: number): void {
    if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    requestAnimationFrame(() =>
      (
        document.querySelector<HTMLElement>(`[data-category-state-action="${id}"]`) ??
        document.getElementById('categoria-status')
      )?.focus(),
    );
  }
}
