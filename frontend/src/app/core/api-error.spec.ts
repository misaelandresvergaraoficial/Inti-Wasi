import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { CategoriaForm } from '../features/categorias/categoria-form';
import { ProductoForm } from '../features/productos/producto-form';
import { toApiError } from './api-error';
import { AuthService } from './auth.service';
import { CategoriasService } from './categorias.service';
import { NoticeService } from './notice.service';
import { ProductosService } from './productos.service';

describe('Errores de API asociados a formularios de catálogo', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('muestra el conflicto de nombre junto al campo de categoría', async () => {
    const message = 'nomCategoria: El nombre de la categoría ya está registrado';
    const save = vi
      .fn()
      .mockRejectedValue(toApiError(new HttpErrorResponse({ status: 409, error: { message } })));
    await TestBed.configureTestingModule({
      imports: [CategoriaForm],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } },
        { provide: CategoriasService, useValue: { save } },
        { provide: AuthService, useValue: { valid: () => true } },
        { provide: NoticeService, useValue: {} },
        { provide: MatDialog, useValue: {} },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(CategoriaForm);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.componentInstance.form.controls.nomCategoria.setValue('Periféricos');
    await fixture.componentInstance.save();
    fixture.detectChanges();

    expect(save).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.form.controls.nomCategoria.getError('server')).toBe(message);
    expect(fixture.nativeElement.querySelector('mat-error')?.textContent).toContain(message);
    expect(
      fixture.nativeElement
        .querySelector('[formControlName="nomCategoria"]')
        .getAttribute('aria-invalid'),
    ).toBe('true');
  });

  it('conserva el campo de usuario y los errores sin campo identificado', () => {
    const user = toApiError(
      new HttpErrorResponse({
        status: 400,
        error: { message: 'nomUsuario: El nombre es obligatorio' },
      }),
    );
    const unknown = toApiError(
      new HttpErrorResponse({
        status: 422,
        error: { message: 'La operación no está disponible' },
      }),
    );
    expect(user.field).toBe('nomUsuario');
    expect(unknown.field).toBeUndefined();
  });

  it.each([
    ['sku', 409, 'sku: El SKU ingresado ya está asignado a otro producto'],
    ['nomProducto', 400, 'nomProducto: El nombre admite letras y números; sin emojis'],
  ] as const)(
    'asocia el error de %s al campo de producto y mueve el foco',
    async (field, status, message) => {
      const save = vi
        .fn()
        .mockRejectedValue(toApiError(new HttpErrorResponse({ status, error: { message } })));
      await TestBed.configureTestingModule({
        imports: [ProductoForm],
        providers: [
          provideRouter([]),
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({}) } } },
          {
            provide: ProductosService,
            useValue: {
              save,
              getCategorias: vi
                .fn()
                .mockResolvedValue([{ idCategoria: 1, nomCategoria: 'Periféricos', estado: 1 }]),
              getProveedores: vi.fn().mockResolvedValue([]),
            },
          },
          { provide: AuthService, useValue: { valid: () => true } },
          { provide: NoticeService, useValue: {} },
          { provide: MatDialog, useValue: {} },
        ],
      }).compileComponents();
      const fixture = TestBed.createComponent(ProductoForm);
      fixture.detectChanges();
      await fixture.componentInstance.init();
      await fixture.whenStable();
      fixture.detectChanges();
      fixture.componentInstance.form.patchValue({
        sku: 'QA-UNICO',
        nomProducto: 'Adaptador USB',
        idCategoria: 1,
      });
      expect(fixture.componentInstance.loading()).toBe(false);
      expect(fixture.componentInstance.form.valid).toBe(true);
      await fixture.componentInstance.save();
      fixture.detectChanges();
      await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
      const input = fixture.nativeElement.querySelector(`[formControlName="${field}"]`);

      expect(save).toHaveBeenCalledOnce();
      expect(fixture.componentInstance.form.controls[field].getError('server')).toBe(message);
      expect(fixture.nativeElement.querySelector('mat-error')?.textContent).toContain(message);
      expect(input.getAttribute('aria-invalid')).toBe('true');
      expect(document.activeElement).toBe(input);
    },
  );
});
