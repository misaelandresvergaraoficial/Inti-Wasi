import { formatBusinessDate } from '../../core/business-time';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { SalidaResponse, estadoDocumentoSalida } from '../../core/salida-models';
import { OrdenEmision } from '../../core/emission-filter';
import { SalidasService } from '../../core/salidas.service';
import { NoticeService } from '../../core/notice.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-salidas-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './salidas-list.html',
})
export class SalidasList {
  private readonly salidas = inject(SalidasService);
  readonly notice = inject(NoticeService);
  readonly records = signal<SalidaResponse[]>([]);
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
  readonly estado = estadoDocumentoSalida;
  readonly total = signal(0);
  readonly pageCount = signal(0);
  readonly filtered = computed(() => this.records());
  readonly visible = computed(() => this.records());
  readonly pages = computed(() => Math.max(1, this.pageCount()));
  readonly rangeEnd = computed(() => this.page() * this.pageSize + this.records().length);
  private requestId = 0;

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    const requestId = ++this.requestId;
    this.error.set('');
    if (this.dateError()) {
      this.records.set([]);
      this.total.set(0);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    try {
      const result = await this.salidas.list(
        {
          q: this.query(),
          estado: this.status(),
          desde: this.desde(),
          hasta: this.hasta(),
          sort: this.sort(),
        },
        this.page(),
        this.pageSize,
      );
      if (requestId !== this.requestId) return;
      this.records.set(result.content);
      this.total.set(result.totalElements);
      this.pageCount.set(result.totalPages);
    } catch (error) {
      if (requestId !== this.requestId) return;
      this.records.set([]);
      this.total.set(0);
      this.error.set(error instanceof Error ? error.message : 'No se pudo consultar el historial.');
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
  go(page: number): void {
    if (this.loading() || page < 0 || page >= this.pages()) return;
    this.page.set(page);
    void this.load();
  }

  date(value: string): string {
    return formatBusinessDate(value, 'medium');
  }
}
