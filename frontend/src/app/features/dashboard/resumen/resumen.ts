import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../services/dashboard';
import { DashboardResumen } from '../models/dashboard.model';

@Component({
  selector: 'app-resumen',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './resumen.html'
})
export class ResumenComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  resumen: DashboardResumen | null = null;
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.cargarResumen();
  }

  cargarResumen(): void {
    this.dashboardService.getResumen().subscribe({
      next: (data) => {
        this.resumen = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'No se pudo cargar la información del panel.';
        this.loading = false;
      }
    });
  }
}