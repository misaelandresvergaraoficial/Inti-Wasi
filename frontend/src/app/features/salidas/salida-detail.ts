import { formatBusinessDate } from '../../core/business-time';
import { tieneEmojis } from '../../shared/form-errors';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { NoticeService } from '../../core/notice.service';
import { SalidaResponse, estadoDocumentoSalida } from '../../core/salida-models';
import { SalidasService } from '../../core/salidas.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-salida-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './salida-detail.html',
})
export class SalidaDetail {
  readonly auth = inject(AuthService);
  private readonly salidas = inject(SalidasService);
  private readonly dialog = inject(MatDialog);
  readonly notice = inject(NoticeService);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly record = signal<SalidaResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly confirming = signal(false);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly motivo = signal('');
  readonly estado = estadoDocumentoSalida;

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      if (!Number.isInteger(this.id) || this.id < 1)
        throw new Error('El número de salida no es válido.');
      this.record.set(await this.salidas.get(this.id));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar la salida.');
    } finally {
      this.loading.set(false);
    }
  }

  date(value: string): string {
    return formatBusinessDate(value, 'long');
  }

  async annul(): Promise<void> {
    if (this.busy() || this.confirming() || this.record()?.estadoDocumento !== 1) return;
    const reason = this.motivo().trim();
    if (tieneEmojis(reason)) {
      this.actionError.set('No uses emojis en el motivo de anulación.');
      document.getElementById('motivo-anulacion')?.focus();
      return;
    }
    this.actionError.set('');
    if (!reason || reason.length > 255) {
      this.actionError.set('Escribe un motivo de anulación de hasta 255 caracteres.');
      document.getElementById('motivo-anulacion')?.focus();
      return;
    }
    this.confirming.set(true);
    let confirmed: boolean;
    try {
      const current = await this.salidas.get(this.id);
      this.record.set(current);
      if (current.estadoDocumento !== 1) {
        this.actionError.set('La operación ya no está vigente. Revisa el estado actualizado.');
        return;
      }
      confirmed = !!(await firstValueFrom(
        this.dialog
          .open(ConfirmDialog, {
            width: '780px',
            maxWidth: 'calc(100vw - 32px)',
            autoFocus: 'first-tabbable',
            data: {
              title: `¿Anular la salida N.° ${this.id}?`,
              message:
                'Se registrará una corrección que devolverá las unidades al stock. La salida original se conservará.',
              action: 'Anular salida',
              danger: true,
              inventory: current.movimientos.map((line) => {
                const cambioNeto = line.cantidad;
                return {
                  producto: line.nomProducto,
                  cantidad: line.cantidad,
                  stockPrevio: line.stockActual,
                  cambioNeto,
                  stockResultado: line.stockActual + cambioNeto,
                  reversion: cambioNeto,
                };
              }),
            },
          })
          .afterClosed(),
      ));
    } catch (error) {
      this.actionError.set(
        error instanceof Error ? error.message : 'No se pudo actualizar el stock para confirmar.',
      );
      return;
    } finally {
      this.confirming.set(false);
    }
    if (!confirmed) return;
    this.busy.set(true);
    try {
      await this.salidas.annul(this.id, reason);
      await this.load();
      this.motivo.set('');
      this.notice.show(`Salida N.° ${this.id} anulada. El historial se conserva.`);
      requestAnimationFrame(() => document.querySelector<HTMLElement>('main h1')?.focus());
    } catch (error) {
      this.actionError.set(error instanceof Error ? error.message : 'No se pudo anular la salida.');
    } finally {
      this.busy.set(false);
    }
  }
}
