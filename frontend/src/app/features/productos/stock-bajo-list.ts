import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductosService } from '../../core/productos.service';
import { ProductoResponse } from '../../core/producto-models';
import { Icon } from '../../shared/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'iw-stock-bajo',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './stock-bajo-list.html',
})
export class StockBajoList {
  private readonly srv = inject(ProductosService);

  readonly records = signal<ProductoResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.records.set(await this.srv.getLowStock());
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'No se pudo consultar el stock bajo.',
      );
    } finally {
      this.loading.set(false);
    }
  }
}
