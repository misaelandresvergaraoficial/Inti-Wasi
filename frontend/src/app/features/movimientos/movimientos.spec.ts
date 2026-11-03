import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { PageEvent } from '@angular/material/paginator';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { MovimientosService } from '../../core/movimientos.service';
import { MovimientoDetail } from './movimiento-detail';
import { MovimientosList } from './movimientos-list';

const movement = {
  idMovimiento: 17,
  idDocumento: 8,
  idDocumentoOrigen: 4,
  estadoDocumento: 2,
  tipoDocumento: 'Correccion' as const,
  fechaEmision: '2026-10-13T14:00:00',
  idUsuario: 1,
  usuarioResponsable: 'Administradora',
  idProducto: 3,
  sku: 'CPU-3',
  nomProducto: 'Procesador',
  nomCategoria: 'Componentes',
  cantidad: 5,
  cantidadConSigno: -5,
  motivo: 'Recepción duplicada',
  idOrden: 2,
  numeroGuiaRemision: 'G-2',
};

describe('Pantallas de Movimientos', () => {
  it('rechaza un rango invertido y pagina los resultados del servidor', async () => {
    const list = vi.fn().mockResolvedValue({
      content: [movement],
      totalElements: 21,
      totalPages: 2,
      number: 0,
    });
    await TestBed.configureTestingModule({
      imports: [MovimientosList],
      providers: [
        provideRouter([]),
        {
          provide: MovimientosService,
          useValue: {
            list,
            productos: vi.fn().mockResolvedValue([]),
            responsables: vi.fn().mockResolvedValue([]),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovimientosList);
    await fixture.componentInstance.load();
    fixture.detectChanges();
    expect(fixture.componentInstance.result()?.content[0].cantidadConSigno).toBe(-5);
    fixture.componentInstance.onPage({ pageIndex: 1, pageSize: 20, length: 21 } as PageEvent);
    await fixture.componentInstance.load();
    expect(list).toHaveBeenLastCalledWith({}, 1, 'desc', 20);

    fixture.componentInstance.changeSort('asc');
    await fixture.componentInstance.load();
    expect(list).toHaveBeenLastCalledWith({}, 0, 'asc', 20);

    fixture.componentInstance.changeFilter('fechaInicial', '2026-10-14');
    fixture.componentInstance.changeFilter('fechaFinal', '2026-10-13');
    expect(fixture.componentInstance.filterError()).toContain('fecha inicial');
    const requestsBefore = list.mock.calls.length;
    await fixture.componentInstance.load();
    expect(list).toHaveBeenCalledTimes(requestsBefore);
  });

  it('muestra el signo y los vínculos del movimiento histórico', async () => {
    await TestBed.configureTestingModule({
      imports: [MovimientoDetail],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '17' })) } },
        { provide: MovimientosService, useValue: { get: vi.fn().mockResolvedValue(movement) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovimientoDetail);
    await fixture.componentInstance.load();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('-5');
    expect(fixture.nativeElement.querySelector('a[href="/documentos/8"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('a[href="/ordenes-compra/2"]')).toBeTruthy();
  });
  it('distingue responsables homónimos por correo e ID y señala al inactivo', async () => {
    const responsables = [
      {
        idUsuario: 7,
        nomUsuario: 'Alex Quispe',
        correoUsuario: 'alex.uno@intiwasi.test',
        estado: 1,
      },
      {
        idUsuario: 8,
        nomUsuario: 'Alex Quispe',
        correoUsuario: 'alex.dos@intiwasi.test',
        estado: 0,
      },
    ];
    await TestBed.configureTestingModule({
      imports: [MovimientosList],
      providers: [
        provideRouter([]),
        {
          provide: MovimientosService,
          useValue: {
            list: vi
              .fn()
              .mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0 }),
            productos: vi.fn().mockResolvedValue([]),
            responsables: vi.fn().mockResolvedValue(responsables),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovimientosList);
    await fixture.componentInstance.loadOptions();
    await fixture.componentInstance.load();
    fixture.detectChanges();
    const options = [
      ...fixture.nativeElement.querySelectorAll('#movimiento-usuario option'),
    ] as HTMLOptionElement[];
    expect(options.find((option) => option.value === '7')?.textContent).toContain(
      'alex.uno@intiwasi.test · ID 7',
    );
    expect(options.find((option) => option.value === '8')?.textContent).toContain(
      'alex.dos@intiwasi.test · ID 8 · Inactivo',
    );
  });

  it('identifica productos inactivos en el filtro histórico', async () => {
    await TestBed.configureTestingModule({
      imports: [MovimientosList],
      providers: [
        provideRouter([]),
        {
          provide: MovimientosService,
          useValue: {
            list: vi
              .fn()
              .mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0 }),
            productos: vi.fn().mockResolvedValue([
              { idProducto: 1, sku: 'A', nomProducto: 'Activo', estado: 1 },
              { idProducto: 2, sku: 'B', nomProducto: 'Histórico', estado: 0 },
            ]),
            responsables: vi.fn().mockResolvedValue([]),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MovimientosList);
    await fixture.componentInstance.loadOptions();
    await fixture.whenStable();
    const active = fixture.nativeElement.querySelector(
      '#movimiento-producto option[value="1"]',
    ) as HTMLOptionElement;
    const inactive = fixture.nativeElement.querySelector(
      '#movimiento-producto option[value="2"]',
    ) as HTMLOptionElement;
    expect(active.textContent).not.toContain('Inactivo');
    expect(inactive.textContent).toContain('B · Histórico · Inactivo');
  });
});
