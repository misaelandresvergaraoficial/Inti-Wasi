import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { DocumentoResponse } from './documento-models';
import { ApiRequestError } from './models';

@Injectable({ providedIn: 'root' })
export class DocumentosService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/documentos';

  list(): Promise<DocumentoResponse[]> {
    return this.request(this.http.get<DocumentoResponse[]>(this.base));
  }

  get(id: number): Promise<DocumentoResponse> {
    return this.request(this.http.get<DocumentoResponse>(this.base + '/' + id));
  }

  private async request<T>(operation: import('rxjs').Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(operation);
    } catch (error) {
      const mapped = toApiError(error);
      if (mapped.status === 401) {
        this.auth.logout(true);
        throw new ApiRequestError('Tu sesión ha caducado. Inicia sesión nuevamente.', 401);
      }
      throw mapped;
    }
  }
}
