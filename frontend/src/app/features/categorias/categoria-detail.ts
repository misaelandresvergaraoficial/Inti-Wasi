import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';
import { CategoriasService } from '../../core/categorias.service';
import { CategoriaResponse } from '../../core/categoria-models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-categoria-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  template: `
    <div class="breadcrumb">
      <a routerLink="/categorias">Categorías</a><iw-icon name="next" /><span>Detalle</span>
    </div>
    <div class="page-heading flex flex-wrap items-start justify-between gap-4">
      <div>
        <div class="eyebrow">CATÁLOGOS</div>
        <h1 tabindex="-1">Detalle de categoría</h1>
        <p>Información actual del catálogo e identidad del registro.</p>
      </div>
      <a mat-stroked-button routerLink="/categorias">Volver al listado</a>
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
            <h2 id="record-heading">{{ r.nomCategoria }}</h2>
            <p>ID {{ r.idCategoria }}</p>
          </div>
          <span class="badge status-badge" [attr.data-state]="r.estado">{{
            r.estado === 1 ? 'Activo' : 'Inactivo'
          }}</span>
        </div>
        <p class="catalog-detail-note">
          La reactivación recupera solo productos desactivados por esta categoría; exige que sus
          proveedores estén activos. El stock y el historial se conservan.
        </p>
        @if (auth.isAdmin()) {
          <div class="form-actions">
            <a mat-flat-button [routerLink]="['/categorias', r.idCategoria, 'editar']"
              ><iw-icon name="edit" />Editar categoría</a
            >
          </div>
        }
      </section>
    }
  `,
})
export class CategoriaDetail {
  private readonly service = inject(CategoriasService);
  readonly auth = inject(AuthService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly validId = Number.isInteger(this.id) && this.id > 0;
  readonly record = signal<CategoriaResponse | null>(null);
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
