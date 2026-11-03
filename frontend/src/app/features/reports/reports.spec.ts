import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { MovimientosService } from '../../core/movimientos.service';
import { ProductosService } from '../../core/productos.service';
import { ReportsService } from '../../core/reports.service';
import { Reports } from './reports';

describe('Reportes', () => {
  it('identifica productos históricos inactivos en el filtro de movimientos', async () => {
    await TestBed.configureTestingModule({
      imports: [Reports],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ tipo: 'movimientos' }) } },
        },
        {
          provide: ReportsService,
          useValue: {
            consultar: vi
              .fn()
              .mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0 }),
          },
        },
        { provide: ProductosService, useValue: { list: vi.fn().mockResolvedValue([]) } },
        {
          provide: MovimientosService,
          useValue: {
            productos: vi.fn().mockResolvedValue([
              { idProducto: 1, sku: 'A', nomProducto: 'Activo', estado: 1 },
              { idProducto: 2, sku: 'B', nomProducto: 'Histórico', estado: 0 },
            ]),
            responsables: vi.fn().mockResolvedValue([]),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(Reports);
    await fixture.componentInstance.loadOptions();
    await fixture.whenStable();
    const active = fixture.nativeElement.querySelector(
      '#report-product option[value="1"]',
    ) as HTMLOptionElement;
    const inactive = fixture.nativeElement.querySelector(
      '#report-product option[value="2"]',
    ) as HTMLOptionElement;
    expect(active.textContent).not.toContain('Inactivo');
    expect(inactive.textContent).toContain('B · Histórico · Inactivo');
  });
});
