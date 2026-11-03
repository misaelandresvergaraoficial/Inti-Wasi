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
  inventory?: {
    producto: string;
    cantidad: number;
    stockPrevio: number;
    cambioNeto: number;
    stockResultado: number;
    reversion?: number;
  }[];
}
@Component({
  selector: 'iw-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule, A11yModule, Icon],
  template: ` <div class="dialog-symbol" [class.danger]="data.danger">
      <iw-icon [name]="data.danger ? 'ban' : 'info'" />
    </div>
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
      @if (data.inventory?.length) {
        <div
          class="table-scroll stock-confirmation"
          role="region"
          aria-label="Resumen del efecto en inventario"
          tabindex="0"
        >
          <table>
            <caption class="sr-only">
              Productos y efecto previsto sobre stock
            </caption>
            <thead>
              <tr>
                <th scope="col">Producto</th>
                <th scope="col" class="numeric">Cantidad</th>
                <th scope="col" class="numeric">Stock previo</th>
                @if (showReversal) {
                  <th scope="col" class="numeric">Reversión</th>
                }
                <th scope="col" class="numeric">Cambio neto</th>
                <th scope="col" class="numeric">Resultado</th>
              </tr>
            </thead>
            <tbody>
              @for (line of data.inventory; track $index) {
                <tr>
                  <th scope="row">{{ line.producto }}</th>
                  <td class="numeric">{{ line.cantidad }}</td>
                  <td class="numeric">{{ line.stockPrevio }}</td>
                  @if (showReversal) {
                    <td class="numeric">{{ line.reversion ?? 0 }}</td>
                  }
                  <td class="numeric">{{ line.cambioNeto > 0 ? '+' : '' }}{{ line.cambioNeto }}</td>
                  <td class="numeric">{{ line.stockResultado }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <p class="field-description">
          El stock es referencial. Al confirmar se volverán a comprobar las existencias y las reglas
          de la operación.
        </p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button [mat-dialog-close]="false" cdkFocusInitial>Cancelar</button>
      <button mat-flat-button [class.danger-button]="data.danger" [mat-dialog-close]="true">
        {{ data.action }}
      </button>
    </mat-dialog-actions>`,
})
export class ConfirmDialog {
  readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
  readonly showReversal =
    this.data.inventory?.some((line) => line.reversion !== undefined) ?? false;
}
