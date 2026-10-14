import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { nombreEstadoDocumento } from '../../core/documento-models';
import { MovimientoConsultaResponse } from '../../core/movimiento-models';
import { MovimientosService } from '../../core/movimientos.service';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-movimiento-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './movimiento-detail.html',
})
export class MovimientoDetail {
  private readonly movimientos = inject(MovimientosService);
  readonly id = signal(0);
  readonly record = signal<MovimientoConsultaResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly estado = nombreEstadoDocumento;
  private requestId = 0;

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.id.set(Number(params.get('id')));
        void this.load();
      });
  }

  async load(): Promise<void> {
    const currentRequest = ++this.requestId;
    this.loading.set(true);
    this.error.set('');
    this.record.set(null);
    try {
      if (!Number.isInteger(this.id()) || this.id() < 1)
        throw new Error('El número de movimiento no es válido.');
      const item = await this.movimientos.get(this.id());
      if (currentRequest === this.requestId) this.record.set(item);
    } catch (error) {
      if (currentRequest !== this.requestId) return;
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar el movimiento.');
    } finally {
      if (currentRequest === this.requestId) this.loading.set(false);
    }
  }

  date(value: string): string {
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' }).format(
      new Date(value),
    );
  }
}
