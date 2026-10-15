import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AjustesService } from '../../core/ajustes.service';
import { AuthService } from '../../core/auth.service';
import {
  DocumentoResponse,
  TipoDocumento,
  nombreEstadoDocumento,
} from '../../core/documento-models';
import { DocumentosService } from '../../core/documentos.service';
import { EntradasService } from '../../core/entradas.service';
import { SalidasService } from '../../core/salidas.service';
import { Icon } from '../../shared/icon';

interface OperationLink {
  label: string;
  path: (string | number)[];
}

@Component({
  selector: 'iw-documento-detail',
  imports: [RouterLink, MatButtonModule, Icon],
  templateUrl: './documento-detail.html',
})
export class DocumentoDetail {
  private readonly documentos = inject(DocumentosService);
  private readonly entradas = inject(EntradasService);
  private readonly salidas = inject(SalidasService);
  private readonly ajustes = inject(AjustesService);
  private readonly auth = inject(AuthService);
  readonly id = signal(0);
  readonly record = signal<DocumentoResponse | null>(null);
  readonly related = signal<DocumentoResponse[]>([]);
  readonly operation = signal<OperationLink | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly relatedError = signal('');
  readonly operationError = signal('');
  readonly estado = nombreEstadoDocumento;
  private requestId = 0;

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.id.set(Number(params.get('id')));
        void this.load();
      });
  }

  async load(): Promise<void> {
    const currentRequest = ++this.requestId;
    this.record.set(null);
    this.related.set([]);
    this.operation.set(null);
    this.loading.set(true);
    this.error.set('');
    this.relatedError.set('');
    this.operationError.set('');
    try {
      if (!Number.isInteger(this.id()) || this.id() < 1)
        throw new Error('El número de documento no es válido.');
      const document = await this.documentos.get(this.id());
      if (currentRequest !== this.requestId) return;
      this.record.set(document);
      let all: DocumentoResponse[];
      try {
        all = await this.documentos.list();
      } catch (error) {
        if (currentRequest !== this.requestId) return;
        this.relatedError.set(
          error instanceof Error ? error.message : 'No se pudieron cargar los vínculos.',
        );
        return;
      }
      if (currentRequest !== this.requestId) return;
      this.related.set(all.filter((item) => item.idDocumentoOrigen === document.idDocumento));
      const source =
        document.tipoDocumento === 'Correccion'
          ? all.find((item) => item.idDocumento === document.idDocumentoOrigen)
          : document;
      if (source) {
        try {
          const link = await this.findOperation(source.tipoDocumento, source.idDocumento);
          if (currentRequest === this.requestId) this.operation.set(link);
        } catch (error) {
          if (currentRequest !== this.requestId) return;
          this.operationError.set(
            error instanceof Error ? error.message : 'No se pudo consultar la operación.',
          );
        }
      }
    } catch (error) {
      if (currentRequest !== this.requestId) return;
      this.error.set(error instanceof Error ? error.message : 'No se pudo cargar el documento.');
    } finally {
      if (currentRequest === this.requestId) this.loading.set(false);
    }
  }

  private async findOperation(
    tipo: TipoDocumento,
    idDocumento: number,
  ): Promise<OperationLink | null> {
    if (tipo === 'Entrada') {
      const item = (await this.entradas.list()).find((entry) => entry.idDocumento === idDocumento);
      return item
        ? { label: 'Ver entrada N.° ' + item.idEntrada, path: ['/entradas', item.idEntrada] }
        : null;
    }
    if (tipo === 'Salida') {
      const item = (await this.salidas.list()).find((exit) => exit.idDocumento === idDocumento);
      return item
        ? { label: 'Ver salida N.° ' + item.idSalida, path: ['/salidas', item.idSalida] }
        : null;
    }
    if (tipo === 'Ajuste') {
      if (!this.auth.isAdmin()) return null;
      const item = (await this.ajustes.list()).find(
        (adjustment) => adjustment.idDocumento === idDocumento,
      );
      return item
        ? { label: 'Ver ajuste N.° ' + item.idAjuste, path: ['/ajustes', item.idAjuste] }
        : null;
    }
    return null;
  }

  date(value: string): string {
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' }).format(
      new Date(value),
    );
  }
}
