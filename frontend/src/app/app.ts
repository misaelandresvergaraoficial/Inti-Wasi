import { Component, effect, inject } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { SessionWarningDialog } from './shared/session-warning-dialog';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  template: '<router-outlet />',
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private warning?: MatDialogRef<SessionWarningDialog>;

  constructor() {
    effect(() => {
      if (this.auth.idleSecondsRemaining() !== null) {
        this.warning ??= this.dialog.open(SessionWarningDialog, {
          width: '460px', maxWidth: 'calc(100vw - 32px)',
          disableClose: true, closeOnNavigation: false, role: 'alertdialog',
        });
      } else if (this.warning) {
        this.warning.close();
        this.warning = undefined;
      }
    });
  }
}
