import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { EntradaResponse, estadoDocumento, fechaEntrada } from '../../core/entrada-models';
import { OrdenEmision } from '../../core/emission-filter';
import { EntradasService } from '../../core/entradas.service';
import { NoticeService } from '../../core/notice.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-entradas-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './entradas-list.html',
})
export class EntradasList {
  private readonly entradas = inject(EntradasService);
  readonly notice = inject(NoticeService);
  readonly records = signal<EntradaResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly query = signal('');
  readonly status = signal('');
  readonly desde = signal('');
  readonly hasta = signal('');
  readonly sort = signal<OrdenEmision>('desc');
  readonly dateError = computed(() =>
    this.desde() && this.hasta() && this.desde() > this.hasta()
      ? 'La fecha inicial no puede ser posterior a la fecha final.'
      : '',
  );
  readonly page = signal(0);
  readonly pageSize = 8;
  readonly estado = estadoDocumento;
  readonly total = signal(0);
  readonly pages = signal(1);
  readonly filtered = computed(() => this.records());
  readonly visible = computed(() => this.records());
  readonly rangeEnd = computed(() => Math.min((this.page() + 1) * this.pageSize, this.total()));
  private requestId = 0;

  changePage(page: number): void {
    this.page.set(page);
    void this.load();
  }

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    const requestId = ++this.requestId;
    if (this.dateError()) {
      this.records.set([]);
      this.total.set(0);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set('');
    try {
      const result = await this.entradas.list({
        page: this.page(),
        size: this.pageSize,
        q: this.query(),
        estado: this.status(),
        desde: this.desde(),
        hasta: this.hasta(),
        sort: this.sort(),
      });
      if (requestId !== this.requestId) return;
      this.records.set(result.content);
      this.total.set(result.totalElements);
      this.pages.set(Math.max(1, result.totalPages));
      if (this.page() >= this.pages()) {
        this.changePage(this.pages() - 1);
      }
    } catch (error) {
      if (requestId !== this.requestId) return;
      this.error.set(
        error instanceof Error ? error.message : 'No se pudieron cargar las entradas.',
      );
    } finally {
      if (requestId === this.requestId) this.loading.set(false);
    }
  }

  search(value: string): void {
    this.query.set(value);
    this.page.set(0);
    void this.load();
  }
  filterStatus(value: string): void {
    this.status.set(value);
    this.page.set(0);
    void this.load();
  }
  changeDate(field: 'desde' | 'hasta', value: string): void {
    (field === 'desde' ? this.desde : this.hasta).set(value);
    this.page.set(0);
    void this.load();
  }
  changeSort(value: OrdenEmision): void {
    this.sort.set(value);
    this.page.set(0);
    void this.load();
  }
  clearFilters(): void {
    this.query.set('');
    this.status.set('');
    this.desde.set('');
    this.hasta.set('');
    this.sort.set('desc');
    this.page.set(0);
    void this.load();
  }
  readonly date = fechaEntrada;
}
