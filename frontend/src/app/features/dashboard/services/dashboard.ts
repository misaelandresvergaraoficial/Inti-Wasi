import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardResumen } from '../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);

  getResumen(): Observable<DashboardResumen> {
    // Apuntamos directamente a la ruta que te funcionó en Thunder Client
    return this.http.get<DashboardResumen>('http://localhost:8080/api/dashboard/resumen');
  }
}