import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductosService } from '../../core/productos.service';
import { ProductoResponse } from '../../core/producto-models';
import { Icon } from '../../shared/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'iw-stock-bajo',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './stock-bajo-list.html'
})
export class StockBajoList {
  private readonly srv = inject(ProductosService);
  
  readonly records = signal<ProductoResponse[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.srv.getLowStock()
      .then(r => this.records.set(r))
      .catch(() => this.records.set([]))
      .finally(() => this.loading.set(false));
  }
}