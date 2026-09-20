import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardResumen } from '../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/dashboard';

  getResumen(): Observable<DashboardResumen> {
    // Gracias a tu Interceptor, esta petición ya lleva el token automáticamente
    return this.http.get<DashboardResumen>(`${this.apiUrl}/resumen`);
  }
}