import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { CategoriaResponse, ProductoResponse } from '../../core/producto-models';
import { ProductosService } from '../../core/productos.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-productos-list',
  imports: [RouterLink, MatButtonModule, Icon, CurrencyPipe],
  templateUrl: './productos-list.html'
})
export class ProductosList {
  private readonly srv = inject(ProductosService);
  private readonly dialog = inject(MatDialog);
  readonly auth = inject(AuthService);
  readonly notice = inject(NoticeService);

  readonly records = signal<ProductoResponse[]>([]);
  readonly categorias = signal<CategoriaResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  
  // Filtros y Paginación
  readonly query = signal('');
  readonly categoriaFilter = signal('');
  readonly statusFilter = signal('');
  readonly page = signal(0);
  readonly pageSize = 10;

  readonly filtered = computed(() =>
    this.records().filter((p) => {
      // 1. Filtrar por Estado (Activos, Inactivos o Todos)
      const matchesStatus = this.statusFilter() === '' || p.estado === Number(this.statusFilter());
      
      // 2. Filtrar por Categoría seleccionada
      const matchesCategoria = this.categoriaFilter() === '' || p.nomCategoria === this.categoriaFilter();

      // 3. Buscar estrictamente por Nombre o SKU
      const searchTerm = this.query().trim().toLowerCase();
      const matchesQuery = !searchTerm || 
        p.sku.toLowerCase().includes(searchTerm) || 
        p.nomProducto.toLowerCase().includes(searchTerm);

      return matchesStatus && matchesCategoria && matchesQuery;
    }),
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
    try {
      const [productos, cats] = await Promise.all([
        this.srv.list(),
        this.srv.getCategorias()
      ]);
      this.records.set(productos);
      this.categorias.set(cats);
      if (this.page() >= this.pages()) this.page.set(this.pages() - 1);
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Error de servidor.');
    } finally {
      this.loading.set(false);
    }
  }

  search(val: string): void {
    this.query.set(val);
    this.page.set(0);
  }

  filterCategoria(val: string): void {
    this.categoriaFilter.set(val);
    this.page.set(0);
  }

  filterStatus(val: string): void {
    this.statusFilter.set(val);
    this.page.set(0);
  }

  reset(): void {
    this.query.set('');
    this.categoriaFilter.set('');
    this.statusFilter.set('');
    this.page.set(0);
  }

  async desactivar(p: ProductoResponse): Promise<void> {
    const confirmed = await firstValueFrom(this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Desactivar Producto',
        message: `¿Estás seguro de desactivar "${p.nomProducto}"? Pasará a estado inactivo.`,
        action: 'Desactivar',
        danger: true
      }
    }).afterClosed());

    if (!confirmed) return;
    
    try {
      await this.srv.delete(p.idProducto);
      this.records.update(curr => curr.map(x => x.idProducto === p.idProducto ? { ...x, estado: 0 } : x));
      this.notice.show('Producto desactivado correctamente.');
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo desactivar el producto.');
    }
  }

  async reactivar(p: ProductoResponse): Promise<void> {
    const confirmed = await firstValueFrom(this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Reactivar Producto',
        message: `¿Estás seguro de reactivar "${p.nomProducto}"? Volverá a estar disponible.`,
        action: 'Reactivar'
      }
    }).afterClosed());

    if (!confirmed) return;

    try {
      await this.srv.activate(p.idProducto);
      this.records.update(curr => curr.map(x => x.idProducto === p.idProducto ? { ...x, estado: 1 } : x));
      this.notice.show('Producto reactivado correctamente.');
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'No se pudo reactivar el producto.');
    }
  }
}