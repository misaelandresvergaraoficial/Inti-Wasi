import { Component, inject } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { A11yModule } from '@angular/cdk/a11y';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'iw-session-warning-dialog',
  imports: [MatDialogModule, MatButtonModule, A11yModule],
  template: `
    <h2 mat-dialog-title>¿Sigues utilizando el sistema?</h2>
    <mat-dialog-content>
      <p>Llevas 15 minutos sin actividad. Confirma para continuar trabajando.</p>
      <p>Tu sesión se cerrará en {{ auth.idleSecondsRemaining() }} segundos si no confirmas.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="auth.logout()">Cerrar sesión</button>
      <button mat-flat-button cdkFocusInitial (click)="auth.continueSession()">Continuar sesión</button>
    </mat-dialog-actions>
  `,
})
export class SessionWarningDialog {
  readonly auth = inject(AuthService);
}
