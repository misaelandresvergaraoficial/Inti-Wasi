import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { SalidaResponse, estadoDocumentoSalida } from '../../core/salida-models';
import { enRangoDeEmision, ordenarPorEmision, OrdenEmision } from '../../core/emission-filter';
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
  readonly filtered = computed(() =>
    this.dateError()
      ? []
      : ordenarPorEmision(
          this.records().filter(
            (record) =>
              (!this.status() || String(record.estadoDocumento) === this.status()) &&
              enRangoDeEmision(record.fechaEmision, this.desde(), this.hasta()) &&
              `${record.idSalida} ${record.motivo} ${record.usuarioResponsable}`
                .toLocaleLowerCase()
                .includes(this.query().trim().toLocaleLowerCase()),
          ),
          this.sort(),
          (record) => record.idSalida,
        ),
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
      this.records.set(await this.salidas.list());
      if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudieron cargar las salidas.');
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
  changeSort(value: OrdenEmision): void {
    this.sort.set(value);
    this.page.set(0);
  }
  clearFilters(): void {
    this.query.set('');
    this.status.set('');
    this.desde.set('');
    this.hasta.set('');
    this.sort.set('desc');
    this.page.set(0);
  }
  date(value: string): string {
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(value),
    );
  }
}
