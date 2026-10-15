import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TipoDocumento, nombreEstadoDocumento } from '../../core/documento-models';
import {
  FiltrosMovimiento,
  PaginaMovimientos,
  OpcionProductoMovimiento,
  OpcionResponsableMovimiento,
} from '../../core/movimiento-models';
import { MovimientosService } from '../../core/movimientos.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-movimientos-list',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './movimientos-list.html',
})
export class MovimientosList {
  private readonly movimientos = inject(MovimientosService);
  readonly filters = signal<FiltrosMovimiento>({});
  readonly products = signal<OpcionProductoMovimiento[]>([]);
  readonly users = signal<OpcionResponsableMovimiento[]>([]);
  readonly sort = signal<'asc' | 'desc'>('desc');
  readonly optionsError = signal('');
  readonly result = signal<PaginaMovimientos | null>(null);
  readonly page = signal(0);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly estado = nombreEstadoDocumento;
  readonly filterError = computed(() => {
    const { fechaInicial, fechaFinal } = this.filters();
    return fechaInicial && fechaFinal && fechaInicial > fechaFinal
      ? 'La fecha inicial no puede ser posterior a la fecha final.'
      : '';
  });
  readonly hasFilters = computed(() => Object.keys(this.filters()).length > 0);
  private requestId = 0;

  constructor() {
    void this.loadOptions();
    void this.load();
  }

  async loadOptions(): Promise<void> {
    this.optionsError.set('');
    const [products, users] = await Promise.allSettled([
      this.movimientos.productos(),
      this.movimientos.responsables(),
    ]);
    if (products.status === 'fulfilled') this.products.set(products.value);
    if (users.status === 'fulfilled') this.users.set(users.value);
    if (products.status === 'rejected' || users.status === 'rejected')
      this.optionsError.set(
        'No se pudieron cargar todos los filtros. Puedes consultar sin ellos o reintentar.',
      );
  }

  changeFilter(key: keyof FiltrosMovimiento, value: string): void {
    const parsed =
      key === 'idProducto' || key === 'idUsuario'
        ? value
          ? Number(value)
          : undefined
        : value || undefined;
    this.filters.update((previous) => {
      const next = { ...previous, [key]: parsed };
      if (parsed === undefined) delete next[key];
      return next;
    });
    this.page.set(0);
    this.result.set(null);
    this.error.set('');
    ++this.requestId;
    this.loading.set(false);
  }

  clearFilters(): void {
    this.filters.set({});
    this.sort.set('desc');
    this.page.set(0);
    void this.load();
  }

  changeSort(value: 'asc' | 'desc'): void {
    this.sort.set(value);
    this.page.set(0);
    void this.load();
  }

  async load(): Promise<void> {
    const currentRequest = ++this.requestId;
    if (this.filterError()) {
      this.result.set(null);
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.error.set('');
    try {
      const result = await this.movimientos.list(this.filters(), this.page(), this.sort());
      if (currentRequest === this.requestId) this.result.set(result);
    } catch (error) {
      if (currentRequest !== this.requestId) return;
      this.result.set(null);
      this.error.set(
        error instanceof Error ? error.message : 'No se pudieron cargar los movimientos.',
      );
    } finally {
      if (currentRequest === this.requestId) this.loading.set(false);
    }
  }

  go(page: number): void {
    const result = this.result();
    if (!result || page < 0 || page >= result.totalPages || this.loading()) return;
    this.page.set(page);
    void this.load();
  }

  date(value: string): string {
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(value),
    );
  }

  type(value: TipoDocumento): string {
    return value === 'Correccion' ? 'Corrección' : value;
  }
}
