import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { ReportsService } from '../../core/reports.service';
import { DashboardResumen } from '../../core/report-models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-dashboard',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly reports = inject(ReportsService);
  readonly data = signal<DashboardResumen | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly maxActividad = computed(() =>
    Math.max(
      1,
      ...(this.data()?.movimientosUltimos30Dias ?? []).flatMap((dia) => [
        dia.entradas,
        dia.salidas,
      ]),
    ),
  );
  readonly hayActividad = computed(() =>
    (this.data()?.movimientosUltimos30Dias ?? []).some(
      (dia) => dia.entradas > 0 || dia.salidas > 0,
    ),
  );

  fechaCorta(fecha: string): string {
    return `${fecha.slice(8, 10)}/${fecha.slice(5, 7)}`;
  }

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.data.set(await this.reports.resumen());
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar el resumen.');
    } finally {
      this.loading.set(false);
    }
  }
}
