import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { DocumentoResponse, nombreEstadoDocumento } from '../../core/documento-models';
import { DocumentosService } from '../../core/documentos.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-documentos-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './documentos-list.html',
})
export class DocumentosList {
  private readonly documentos = inject(DocumentosService);
  readonly records = signal<DocumentoResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly query = signal('');
  readonly tipo = signal('');
  readonly estadoFiltro = signal('');
  readonly page = signal(0);
  readonly pageSize = 10;
  readonly estado = nombreEstadoDocumento;
  readonly filtered = computed(() => {
    const search = this.query().trim().toLocaleLowerCase();
    return this.records().filter(
      (item) =>
        (!this.tipo() || item.tipoDocumento === this.tipo()) &&
        (!this.estadoFiltro() || String(item.estado) === this.estadoFiltro()) &&
        (String(item.idDocumento).includes(search) ||
          item.usuarioResponsable.toLocaleLowerCase().includes(search) ||
          String(item.idDocumentoOrigen ?? '').includes(search)),
    );
  });
  readonly pages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize)));
  readonly visible = computed(() =>
    this.filtered().slice(this.page() * this.pageSize, (this.page() + 1) * this.pageSize),
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
      this.records.set(await this.documentos.list());
      if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    } catch (error) {
      this.records.set([]);
      this.error.set(
        error instanceof Error ? error.message : 'No se pudieron cargar los documentos.',
      );
    } finally {
      this.loading.set(false);
    }
  }

  changeFilter(key: 'query' | 'tipo' | 'estado', value: string): void {
    if (key === 'query') this.query.set(value);
    if (key === 'tipo') this.tipo.set(value);
    if (key === 'estado') this.estadoFiltro.set(value);
    this.page.set(0);
  }

  clearFilters(): void {
    this.query.set('');
    this.tipo.set('');
    this.estadoFiltro.set('');
    this.page.set(0);
  }

  date(value: string): string {
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(value),
    );
  }
}
