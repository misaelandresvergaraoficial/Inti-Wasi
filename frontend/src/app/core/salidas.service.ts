import { Pagina } from './pagination';
import { FiltrosHistorialInventario } from './inventario-history-models';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { toApiError } from './api-error';
import { SalidaRequest, SalidaResponse } from './salida-models';

@Injectable({ providedIn: 'root' })
export class SalidasService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/salidas';

  list(
    filtros: FiltrosHistorialInventario = {},
    page = 0,
    size = 20,
  ): Promise<Pagina<SalidaResponse>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', filtros.sort ?? 'desc');
    for (const [key, value] of Object.entries(filtros)) {
      if (value !== undefined && value !== '') params = params.set(key, value);
    }
    return this.request(this.http.get<Pagina<SalidaResponse>>(this.base, { params }));
  }

  get(id: number): Promise<SalidaResponse> {
    return this.request(this.http.get<SalidaResponse>(`${this.base}/${id}`));
  }

  byDocument(idDocumento: number): Promise<SalidaResponse> {
    return this.request(this.http.get<SalidaResponse>(`${this.base}/documento/${idDocumento}`));
  }

  save(data: SalidaRequest, id?: number): Promise<SalidaResponse> {
    return this.request(
      id === undefined
        ? this.http.post<SalidaResponse>(this.base, data)
        : this.http.put<SalidaResponse>(`${this.base}/${id}`, data),
    );
  }

  annul(id: number, motivo: string): Promise<void> {
    return this.request(this.http.delete<void>(`${this.base}/${id}`, { body: { motivo } }));
  }

  private async request<T>(operation: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(operation);
    } catch (error) {
      throw toApiError(error);
    }
  }
}
