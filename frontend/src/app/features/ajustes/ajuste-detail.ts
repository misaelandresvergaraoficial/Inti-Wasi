import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AjusteResponse, estadoDocumentoAjuste } from '../../core/ajuste-models';
import { AjustesService } from '../../core/ajustes.service';
import { NoticeService } from '../../core/notice.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-ajuste-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './ajuste-detail.html',
})
export class AjusteDetail {
  private readonly ajustes = inject(AjustesService);
  private readonly dialog = inject(MatDialog);
  readonly notice = inject(NoticeService);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly record = signal<AjusteResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly motivo = signal('');
  readonly estado = estadoDocumentoAjuste;

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      if (!Number.isInteger(this.id) || this.id < 1)
        throw new Error('El número de ajuste no es válido.');
      this.record.set(await this.ajustes.get(this.id));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar el ajuste.');
    } finally {
      this.loading.set(false);
    }
  }

  date(value: string): string {
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' }).format(
      new Date(value),
    );
  }

  reversalRisk(item: AjusteResponse): boolean {
    return (
      item.tipoAjuste === 'Incremento' &&
      item.movimientos.some((line) => line.stockActual < line.cantidad)
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
            title: `¿Anular el ajuste N.° ${this.id}?`,
            message:
              'Se registrará una corrección con el efecto contrario. Si al revertir un incremento faltara stock, el servidor rechazará toda la operación.',
            action: 'Anular ajuste',
            danger: true,
          },
        })
        .afterClosed(),
    );
    if (!confirmed) return;
    this.busy.set(true);
    try {
      await this.ajustes.annul(this.id, reason);
      await this.load();
      this.motivo.set('');
      this.notice.show(`Ajuste N.° ${this.id} anulado. El historial se conserva.`);
      requestAnimationFrame(() => document.querySelector<HTMLElement>('main h1')?.focus());
    } catch (error) {
      this.actionError.set(error instanceof Error ? error.message : 'No se pudo anular el ajuste.');
    } finally {
      this.busy.set(false);
    }
  }
}
