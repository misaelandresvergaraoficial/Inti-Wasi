import { Pagina } from './pagination';
import { FiltrosHistorialInventario } from './inventario-history-models';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { toApiError } from './api-error';
import { DocumentoResponse } from './documento-models';

@Injectable({ providedIn: 'root' })
export class DocumentosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/documentos';

  list(
    filtros: FiltrosHistorialInventario = {},
    page = 0,
    size = 20,
  ): Promise<Pagina<DocumentoResponse>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', filtros.sort ?? 'desc');
    for (const [key, value] of Object.entries(filtros)) {
      if (value !== undefined && value !== '') params = params.set(key, value);
    }
    return this.request(this.http.get<Pagina<DocumentoResponse>>(this.base, { params }));
  }

  get(id: number): Promise<DocumentoResponse> {
    return this.request(this.http.get<DocumentoResponse>(this.base + '/' + id));
  }

  related(id: number): Promise<DocumentoResponse[]> {
    return this.request(this.http.get<DocumentoResponse[]>(`${this.base}/${id}/relacionados`));
  }

  private async request<T>(operation: import('rxjs').Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(operation);
    } catch (error) {
      throw toApiError(error);
    }
  }
}
