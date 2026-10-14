import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { EntradaResponse, estadoDocumento } from '../../core/entrada-models';
import { EntradasService } from '../../core/entradas.service';
import { NoticeService } from '../../core/notice.service';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'iw-entrada-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './entrada-detail.html',
})
export class EntradaDetail {
  readonly auth = inject(AuthService);
  private readonly entradas = inject(EntradasService);
  private readonly dialog = inject(MatDialog);
  readonly notice = inject(NoticeService);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly record = signal<EntradaResponse | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly motivo = signal('');
  readonly estado = estadoDocumento;

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      if (!Number.isInteger(this.id) || this.id < 1)
        throw new Error('El número de entrada no es válido.');
      this.record.set(await this.entradas.get(this.id));
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar la entrada.');
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
            title: `¿Anular la entrada N.° ${this.id}?`,
            message:
              'Se registrará una corrección que descontará las unidades de esta entrada. Si el stock actual no alcanza, el servidor rechazará toda la operación.',
            action: 'Anular entrada',
            danger: true,
          },
        })
        .afterClosed(),
    );
    if (!confirmed) return;
    this.busy.set(true);
    try {
      await this.entradas.annul(this.id, reason);
      await this.load();
      this.motivo.set('');
      this.notice.show(`Entrada N.° ${this.id} anulada. El historial se conserva.`);
      requestAnimationFrame(() => document.querySelector<HTMLElement>('main h1')?.focus());
    } catch (error) {
      this.actionError.set(
        error instanceof Error ? error.message : 'No se pudo anular la entrada.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
