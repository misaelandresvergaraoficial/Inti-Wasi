import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AjustesService } from '../../core/ajustes.service';
import { AuthService } from '../../core/auth.service';
import { DocumentoResponse } from '../../core/documento-models';
import { DocumentosService } from '../../core/documentos.service';
import { EntradasService } from '../../core/entradas.service';
import { SalidasService } from '../../core/salidas.service';
import { DocumentoDetail } from './documento-detail';
import { DocumentosList } from './documentos-list';

const original: DocumentoResponse = {
  idDocumento: 4,
  tipoDocumento: 'Entrada',
  fechaEmision: '2026-10-12T10:00:00',
  estado: 2,
  idDocumentoOrigen: null,
  motivoCorreccion: null,
  idUsuario: 2,
  usuarioResponsable: 'Operador',
};
const correction: DocumentoResponse = {
  ...original,
  idDocumento: 7,
  tipoDocumento: 'Correccion',
  estado: 1,
  idDocumentoOrigen: 4,
  motivoCorreccion: 'Cantidad equivocada',
};
const replacement: DocumentoResponse = {
  ...original,
  idDocumento: 8,
  estado: 1,
  idDocumentoOrigen: 4,
};
const documents = [original, correction, replacement];

describe('Pantallas de Documentos', () => {
  it('envía tipo y estado al servidor y conserva total y página de respuesta', async () => {
    const list = vi
      .fn()
      .mockResolvedValue({ content: documents, totalElements: 33, totalPages: 4, number: 0 });
    await TestBed.configureTestingModule({
      imports: [DocumentosList],
      providers: [provideRouter([]), { provide: DocumentosService, useValue: { list } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(DocumentosList);
    await fixture.componentInstance.load();
    expect(fixture.componentInstance.total()).toBe(33);
    list.mockResolvedValue({ content: [correction], totalElements: 1, totalPages: 1, number: 0 });
    fixture.componentInstance.changeFilter('tipo', 'Correccion');
    await fixture.componentInstance.load();
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ tipo: 'Correccion' }), 0, 10);
    expect(fixture.componentInstance.visible().map((item) => item.idDocumento)).toEqual([7]);
    fixture.componentInstance.changeFilter('estado', '0');
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ estado: '0' }), 0, 10);
  });

  it('muestra la corrección y el reemplazo vinculados al original', async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentoDetail],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAdmin: () => true } },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ id: '4' })) },
        },
        {
          provide: DocumentosService,
          useValue: {
            get: vi.fn().mockResolvedValue(original),
            related: vi.fn().mockResolvedValue([correction, replacement]),
          },
        },
        {
          provide: EntradasService,
          useValue: { byDocument: vi.fn().mockResolvedValue({ idDocumento: 4, idEntrada: 3 }) },
        },
        { provide: SalidasService, useValue: { list: vi.fn() } },
        { provide: AjustesService, useValue: { list: vi.fn() } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(DocumentoDetail);
    await fixture.componentInstance.load();
    fixture.detectChanges();
    expect(fixture.componentInstance.related().map((item) => item.idDocumento)).toEqual([7, 8]);
    expect(fixture.componentInstance.operation()).toEqual({
      label: 'Ver entrada N.° 3',
      path: ['/entradas', 3],
    });
    expect(fixture.nativeElement.textContent).toContain('Documentos vinculados');
  });

  it('conserva los vínculos históricos si falla la consulta de la operación', async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentoDetail],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAdmin: () => true } },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '4' })) } },
        {
          provide: DocumentosService,
          useValue: {
            get: vi.fn().mockResolvedValue(original),
            related: vi.fn().mockResolvedValue([correction, replacement]),
          },
        },
        {
          provide: EntradasService,
          useValue: { byDocument: vi.fn().mockRejectedValue(new Error('Sin conexión')) },
        },
        { provide: SalidasService, useValue: { list: vi.fn() } },
        { provide: AjustesService, useValue: { list: vi.fn() } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(DocumentoDetail);
    await fixture.componentInstance.load();
    fixture.detectChanges();
    expect(fixture.componentInstance.related().map((item) => item.idDocumento)).toEqual([7, 8]);
    expect(fixture.componentInstance.operationError()).toBe('Sin conexión');
    expect(fixture.nativeElement.textContent).toContain('N.° 7');
  });

  it('permite consultar un documento de ajuste sin abrir Ajustes como operador', async () => {
    const adjustment = { ...original, tipoDocumento: 'Ajuste' as const };
    const listAdjustments = vi.fn();
    await TestBed.configureTestingModule({
      imports: [DocumentoDetail],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAdmin: () => false } },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '4' })) } },
        {
          provide: DocumentosService,
          useValue: {
            get: vi.fn().mockResolvedValue(adjustment),
            related: vi.fn().mockResolvedValue([]),
          },
        },
        { provide: EntradasService, useValue: { list: vi.fn() } },
        { provide: SalidasService, useValue: { list: vi.fn() } },
        { provide: AjustesService, useValue: { list: listAdjustments } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(DocumentoDetail);
    await fixture.componentInstance.load();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Documento N.° 4');
    expect(fixture.componentInstance.operationError()).toBe('');
    expect(listAdjustments).not.toHaveBeenCalled();
  });
});
