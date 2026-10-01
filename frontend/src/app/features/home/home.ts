import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'iw-home',
  imports: [RouterLink, MatButtonModule, Icon],
  template: ` <div class="breadcrumb">Espacio de trabajo</div>
    <div class="page-heading">
      <div class="eyebrow">INTI WASI</div>
      <h1 tabindex="-1">Hola, {{ auth.session()?.nombre?.split(' ')?.[0] }}.</h1>
      <p>Este es tu espacio de trabajo en el almacén.</p>
    </div>
    <section class="panel welcome-panel">
      <span class="section-icon"><iw-icon name="shield" /></span>
      <h2>Tu cuenta está activa</h2>
      <p>
        Has iniciado sesión como <strong>{{ auth.session()?.rol }}</strong
        >.
      </p>
      <dl class="account-details">
        <div>
          <dt>Nombre</dt>
          <dd>{{ auth.session()?.nombre }}</dd>
        </div>
        <div>
          <dt>Correo electrónico</dt>
          <dd>{{ auth.session()?.correo }}</dd>
        </div>
      </dl>
      @if (auth.isAdmin()) {
        <a mat-flat-button routerLink="/usuarios"><iw-icon name="users" />Gestionar usuarios</a>
      } @else {
        <div class="notice info">
          <iw-icon name="info" />
          <p>
            Tu rol permite consultar productos, proveedores y órdenes, y trabajar con entradas y
            salidas de mercadería. Si necesitas cambiar tu acceso, comunícate con el administrador.
          </p>
        </div>
      }
    </section>`,
})
export class Home {
  readonly auth = inject(AuthService);
}
