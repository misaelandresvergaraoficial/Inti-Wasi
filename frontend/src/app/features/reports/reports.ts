import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { ProductosService } from '../../core/productos.service';
import { UsersService } from '../../core/users.service';
import { ProductoResponse } from '../../core/producto-models';
import { UsuarioResponse } from '../../core/models';
import { ReportsService } from '../../core/reports.service';
import {
  FiltrosReporte,
  FilaReporte,
  InventarioActual,
  MovimientoReporte,
  Pagina,
  StockBajo,
  TipoReporte,
} from '../../core/report-models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-reports',
  imports: [MatButtonModule, Icon],
  templateUrl: './reports.html',
})
export class Reports {
  private readonly reports = inject(ReportsService);
  private readonly productsService = inject(ProductosService);
  private readonly usersService = inject(UsersService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly tipo = signal<TipoReporte>(this.initialType());
  readonly filtros = signal<FiltrosReporte>({});
  readonly products = signal<ProductoResponse[]>([]);
  readonly users = signal<UsuarioResponse[]>([]);
  readonly result = signal<Pagina<FilaReporte> | null>(null);
  readonly page = signal(0);
  readonly loading = signal(true);
  readonly exporting = signal(false);
  readonly error = signal('');
  readonly optionsError = signal('');
  readonly filterError = computed(() => {
    const { fechaInicial, fechaFinal } = this.filtros();
    return fechaInicial && fechaFinal && fechaInicial > fechaFinal
      ? 'La fecha inicial no puede ser posterior a la fecha final.'
      : '';
  });
  readonly descriptions: Record<TipoReporte, string> = {
    inventario: 'Existencias, proveedor y precio de compra referencial de productos activos.',
    movimientos: 'Historial de entradas, salidas, ajustes y correcciones registradas.',
    reposicion: 'Productos que llegaron a su stock mínimo y unidades por reponer.',
  };
  readonly hasFilters = computed(() =>
    Object.values(this.filtros()).some((value) => value !== undefined && value !== ''),
  );

  constructor() {
    void this.loadOptions();
    void this.load();
  }

  private initialType(): TipoReporte {
    const tipo = this.route.snapshot.queryParamMap.get('tipo');
    return tipo === 'movimientos' || tipo === 'reposicion' ? tipo : 'inventario';
  }

  async loadOptions(): Promise<void> {
    this.optionsError.set('');
    try {
      const [products, users] = await Promise.all([
        this.productsService.list(),
        this.usersService.list(),
      ]);
      this.products.set(products);
      this.users.set(users);
    } catch (error) {
      this.optionsError.set(
        error instanceof Error ? error.message : 'No se pudieron cargar los filtros.',
      );
    }
  }

  changeType(value: string): void {
    if (value !== 'inventario' && value !== 'movimientos' && value !== 'reposicion') return;
    if (value === this.tipo()) return;
    this.tipo.set(value);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tipo: value },
      replaceUrl: true,
    });
    this.filtros.set({});
    this.page.set(0);
    void this.load();
  }

  changeFilter(clave: keyof FiltrosReporte, value: string): void {
    const valor =
      clave === 'idProducto' || clave === 'idUsuario'
        ? value
          ? Number(value)
          : undefined
        : value || undefined;
    this.filtros.update((prev) => ({ ...prev, [clave]: valor }));
    this.page.set(0);
    this.result.set(null);
    this.error.set('');
  }

  clearFilters(): void {
    this.filtros.set({});
    this.page.set(0);
    void this.load();
  }

  async load(): Promise<void> {
    if (this.filterError()) return;
    this.loading.set(true);
    this.error.set('');
    try {
      this.result.set(await this.reports.consultar(this.tipo(), this.filtros(), this.page()));
    } catch (error) {
      this.result.set(null);
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar el reporte.');
    } finally {
      this.loading.set(false);
    }
  }

  go(page: number): void {
    this.page.set(page);
    void this.load();
  }

  async export(format: 'pdf' | 'csv'): Promise<void> {
    if (this.exporting() || !this.result()?.totalElements || this.filterError()) return;
    this.exporting.set(true);
    this.error.set('');
    try {
      const response = await this.reports.exportar(this.tipo(), format, this.filtros());
      if (response.status === 204 || !response.body?.size) {
        this.error.set('No hay registros para exportar con estos filtros.');
        return;
      }
      const url = URL.createObjectURL(response.body);
      const link = document.createElement('a');
      link.href = url;
      link.download = `reporte-${this.tipo()}.${format}`;
      document.body.append(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo exportar el reporte.');
    } finally {
      this.exporting.set(false);
    }
  }

  inventario(row: FilaReporte): InventarioActual {
    return row as InventarioActual;
  }
  movimiento(row: FilaReporte): MovimientoReporte {
    return row as MovimientoReporte;
  }
  reposicion(row: FilaReporte): StockBajo {
    return row as StockBajo;
  }
}
