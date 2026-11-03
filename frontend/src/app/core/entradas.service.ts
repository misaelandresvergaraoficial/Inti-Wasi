import { Pagina } from './pagination';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { toApiError } from './api-error';
import { EntradaRequest, EntradaResponse } from './entrada-models';

@Injectable({ providedIn: 'root' })
export class EntradasService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/entradas';

  list(
    filters: {
      page?: number;
      size?: number;
      q?: string;
      estado?: string;
      desde?: string;
      hasta?: string;
      sort?: string;
    } = {},
  ): Promise<Pagina<EntradaResponse>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== '') params = params.set(key, String(value));
    }
    return this.request(this.http.get<Pagina<EntradaResponse>>(this.base, { params }));
  }

  byDocument(idDocumento: number): Promise<EntradaResponse> {
    return this.request(this.http.get<EntradaResponse>(`${this.base}/documento/${idDocumento}`));
  }

  get(id: number): Promise<EntradaResponse> {
    return this.request(this.http.get<EntradaResponse>(`${this.base}/${id}`));
  }

  save(data: EntradaRequest, id?: number): Promise<EntradaResponse> {
    return this.request(
      id === undefined
        ? this.http.post<EntradaResponse>(this.base, data)
        : this.http.put<EntradaResponse>(`${this.base}/${id}`, data),
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
