import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth.service';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'iw-access-denied',
  imports: [RouterLink, MatButtonModule, Icon],
  template: ` <section class="panel access-state">
    <span class="section-icon"><iw-icon name="lock" /></span>
    <div class="eyebrow">ACCESO RESTRINGIDO</div>
    <h1 tabindex="-1">Esta sección requiere otro permiso</h1>
    <p>
      Tu rol de {{ auth.session()?.rol }} no tiene acceso a esta página. Si necesitas ingresar,
      comunícate con el administrador.
    </p>
    <a mat-flat-button [routerLink]="auth.landingRoute()"
      >Volver a mi espacio</a
    >
  </section>`,
})
export class AccessDenied {
  readonly auth = inject(AuthService);
}
