import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';
import { UsersService } from '../../core/users.service';
import { UsuarioResponse } from '../../core/models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-user-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  template: `
    <div class="breadcrumb">
      <a routerLink="/usuarios">Usuarios</a><iw-icon name="next" /><span>Detalle</span>
    </div>
    <div class="page-heading flex flex-wrap items-start justify-between gap-4">
      <div>
        <div class="eyebrow">CATÁLOGOS</div>
        <h1 tabindex="-1">Detalle de usuario</h1>
        <p>Información actual del catálogo e identidad del registro.</p>
      </div>
      <a mat-stroked-button routerLink="/usuarios">Volver al listado</a>
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
            <h2 id="record-heading">{{ r.nomUsuario }}</h2>
            <p>ID {{ r.idUsuario }}</p>
          </div>
          <span class="badge status-badge" [attr.data-state]="r.estado">{{
            r.estado === 1 ? 'Activo' : 'Inactivo'
          }}</span>
        </div>
        <dl class="catalog-detail-grid">
          <div>
            <dt>Correo</dt>
            <dd>{{ r.correo }}</dd>
          </div>
          <div>
            <dt>Rol</dt>
            <dd>{{ r.rol }}</dd>
          </div>
          <div>
            <dt>Teléfono</dt>
            <dd>{{ r.telefono || 'Sin teléfono' }}</dd>
          </div>
        </dl>
        <p class="catalog-detail-note">
          Los cambios de acceso conservan los registros históricos del responsable.
        </p>
        @if (auth.isAdmin()) {
          <div class="form-actions">
            <a mat-flat-button [routerLink]="['/usuarios', r.idUsuario, 'editar']"
              ><iw-icon name="edit" />Editar usuario</a
            >
          </div>
        }
      </section>
    }
  `,
})
export class UserDetail {
  private readonly service = inject(UsersService);
  readonly auth = inject(AuthService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly validId = Number.isInteger(this.id) && this.id > 0;
  readonly record = signal<UsuarioResponse | null>(null);
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
