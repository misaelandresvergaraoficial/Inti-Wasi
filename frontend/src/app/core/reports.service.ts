import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError } from './models';
import { DashboardResumen, FiltrosReporte, FilaReporte, Pagina, TipoReporte } from './report-models';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  resumen(): Promise<DashboardResumen> {
    return this.request(this.http.get<DashboardResumen>('/api/dashboard/resumen'));
  }

  consultar(tipo: TipoReporte, filtros: FiltrosReporte, pagina: number): Promise<Pagina<FilaReporte>> {
    const params = this.params(filtros).set('page', pagina).set('size', 20);
    return this.request(this.http.get<Pagina<FilaReporte>>(`/api/reportes/${tipo}`, { params }));
  }

  exportar(tipo: TipoReporte, formato: 'pdf' | 'excel', filtros: FiltrosReporte): Promise<HttpResponse<Blob>> {
    const params = this.params(filtros).set('formato', formato);
    return this.request(this.http.get(`/api/reportes/${tipo}/exportar`, {
      params, observe: 'response', responseType: 'blob',
    }));
  }

  private params(filtros: FiltrosReporte): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== '') params = params.set(clave, String(valor));
    }
    return params;
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
