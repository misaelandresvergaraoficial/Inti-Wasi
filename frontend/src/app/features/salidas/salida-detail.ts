import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
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
  private readonly salidas = inject(SalidasService);
  private readonly dialog = inject(MatDialog);
  readonly notice = inject(NoticeService);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly record = signal<SalidaResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
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
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' }).format(
      new Date(value),
    );
  }

  async annul(): Promise<void> {
    if (this.busy() || this.record()?.estadoDocumento !== 1) return;
    const reason = this.motivo().trim();
    this.actionError.set('');
    if (!reason || reason.length > 255) {
      this.actionError.set('Escribe un motivo de anulación de hasta 255 caracteres.');
      document.getElementById('motivo-anulacion')?.focus();
      return;
    }
    const confirmed = await firstValueFrom(
      this.dialog
        .open(ConfirmDialog, {
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
          autoFocus: 'first-tabbable',
          data: {
            title: `¿Anular la salida N.° ${this.id}?`,
            message:
              'Se registrará una corrección que devolverá las unidades al stock. La salida original se conservará.',
            action: 'Anular salida',
            danger: true,
          },
        })
        .afterClosed(),
    );
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
