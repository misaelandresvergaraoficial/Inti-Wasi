import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { A11yModule } from '@angular/cdk/a11y';
import { Icon } from './icon';
export interface ConfirmData {
  title: string;
  message: string;
  action: string;
  danger?: boolean;
}
@Component({
  selector: 'iw-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule, A11yModule, Icon],
  template: ` <div class="dialog-symbol" [class.danger]="data.danger">
      <iw-icon [name]="data.danger ? 'ban' : 'info'" />
    </div>
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content
      ><p>{{ data.message }}</p></mat-dialog-content
    >
    <mat-dialog-actions align="end">
      <button mat-stroked-button [mat-dialog-close]="false" cdkFocusInitial>Cancelar</button>
      <button mat-flat-button [class.danger-button]="data.danger" [mat-dialog-close]="true">
        {{ data.action }}
      </button>
    </mat-dialog-actions>`,
})
export class ConfirmDialog {
  readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
}
