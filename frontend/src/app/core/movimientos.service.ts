import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError } from './models';
import {
  FiltrosMovimiento,
  MovimientoConsultaResponse,
  PaginaMovimientos,
  OpcionProductoMovimiento,
  OpcionResponsableMovimiento,
} from './movimiento-models';

@Injectable({ providedIn: 'root' })
export class MovimientosService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/movimientos';

  list(
    filtros: FiltrosMovimiento,
    page: number,
    orden: 'asc' | 'desc' = 'desc',
    size = 20,
  ): Promise<PaginaMovimientos> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .append('sort', `fechaEmision,${orden}`)
      .append('sort', `idMovimiento,${orden}`);
    for (const [key, value] of Object.entries(filtros)) {
      if (value !== undefined && value !== '') params = params.set(key, String(value));
    }
    return this.request(this.http.get<PaginaMovimientos>(this.base, { params }));
  }

  responsables(): Promise<OpcionResponsableMovimiento[]> {
    return this.request(this.http.get<OpcionResponsableMovimiento[]>(`${this.base}/responsables`));
  }

  productos(): Promise<OpcionProductoMovimiento[]> {
    return this.request(this.http.get<OpcionProductoMovimiento[]>(`${this.base}/productos`));
  }

  get(id: number): Promise<MovimientoConsultaResponse> {
    return this.request(this.http.get<MovimientoConsultaResponse>(this.base + '/' + id));
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
