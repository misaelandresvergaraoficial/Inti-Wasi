import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';
import { ProductosService } from '../../core/productos.service';
import { ProductoResponse } from '../../core/producto-models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-producto-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  template: `
    <div class="breadcrumb">
      <a routerLink="/productos">Productos</a><iw-icon name="next" /><span>Detalle</span>
    </div>
    <div class="page-heading flex flex-wrap items-start justify-between gap-4">
      <div>
        <div class="eyebrow">CATÁLOGOS</div>
        <h1 tabindex="-1">Detalle de producto</h1>
        <p>Información actual del catálogo e identidad del registro.</p>
      </div>
      <a mat-stroked-button routerLink="/productos">Volver al listado</a>
    </div>
    @if (loading()) {
      <div class="panel empty-state" role="status">Cargando información…</div>
    } @else if (error()) {
      <div class="notice error" role="alert">
        <iw-icon name="info" />
        <p>{{ error() }}</p>
      </div>
      @if (validId) {
        <button mat-button (click)="load()">Reintentar</button>
      }
    } @else if (record(); as r) {
      <section class="panel catalog-detail" aria-labelledby="record-heading">
        <div class="panel-heading">
          <div>
            <h2 id="record-heading">{{ r.nomProducto }}</h2>
            <p>ID {{ r.idProducto }}</p>
          </div>
          <span class="badge status-badge" [attr.data-state]="r.estado">{{
            r.estado === 1 ? 'Activo' : 'Inactivo'
          }}</span>
        </div>
        <dl class="catalog-detail-grid">
          <div>
            <dt>SKU</dt>
            <dd>{{ r.sku }}</dd>
          </div>
          <div>
            <dt>Categoría</dt>
            <dd>{{ r.nomCategoria }}</dd>
          </div>
          <div>
            <dt>Proveedor</dt>
            <dd>{{ r.nomProveedor || 'Sin proveedor asignado' }}</dd>
          </div>
          <div>
            <dt>Stock actual</dt>
            <dd>{{ r.stockActual }}</dd>
          </div>
          <div>
            <dt>Stock mínimo</dt>
            <dd>{{ r.stockMinimo }}</dd>
          </div>
          <div>
            <dt>Precio de compra referencial</dt>
            <dd>{{ price(r.precio) }}</dd>
          </div>
        </dl>
        <p class="catalog-detail-note">
          El stock cambia mediante entradas, salidas, ajustes y correcciones. El precio es
          referencial para órdenes nuevas; los importes pactados se conservan.
        </p>
        @if (auth.isAdmin()) {
          <div class="form-actions">
            <a mat-flat-button [routerLink]="['/productos', r.idProducto, 'editar']"
              ><iw-icon name="edit" />Editar producto</a
            >
          </div>
        }
      </section>
    }
  `,
})
export class ProductoDetail {
  private readonly service = inject(ProductosService);
  readonly auth = inject(AuthService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly validId = Number.isInteger(this.id) && this.id > 0;
  readonly record = signal<ProductoResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly price = (value: number) =>
    new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value);

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    if (!this.validId) {
      this.loading.set(false);
      this.error.set('El identificador no es válido. Vuelve al listado y selecciona el registro.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    try {
      this.record.set(await this.service.get(this.id));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar el registro.');
    } finally {
      this.loading.set(false);
    }
  }
}
