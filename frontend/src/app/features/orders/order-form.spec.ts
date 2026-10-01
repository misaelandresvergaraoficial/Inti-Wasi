import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { OrdenResponse, ProductoOpcion } from '../../core/order-models';
import { OrdersService } from '../../core/orders.service';
import { OrderForm } from './order-form';

const products: ProductoOpcion[] = [
  {
    idProducto: 12,
    sku: 'CPU-12',
    nomProducto: 'Procesador',
    idProveedor: 1,
    precio: 680,
    estado: 1,
  },
  { idProducto: 13, sku: 'RAM-13', nomProducto: 'Memoria', idProveedor: 2, precio: 190, estado: 1 },
  {
    idProducto: 14,
    sku: 'SSD-14',
    nomProducto: 'Unidad SSD',
    idProveedor: null,
    precio: 240,
    estado: 1,
  },
];

async function setup(existing?: OrdenResponse) {
  const orders = {
    suppliers: vi.fn().mockResolvedValue([
      { idProveedor: 1, nomProveedor: 'Proveedor Uno', estado: 1 },
      { idProveedor: 2, nomProveedor: 'Proveedor Dos', estado: 1 },
    ]),
    products: vi.fn().mockResolvedValue(products),
    get: vi.fn().mockResolvedValue(existing),
    save: vi.fn(),
  };
  await TestBed.configureTestingModule({
    imports: [OrderForm],
    providers: [
      provideRouter([]),
      { provide: OrdersService, useValue: orders },
      { provide: AuthService, useValue: { valid: () => true } },
      { provide: MatDialog, useValue: {} },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { paramMap: convertToParamMap(existing ? { id: '4' } : {}) } },
      },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(OrderForm);
  await fixture.componentInstance.load();
  fixture.detectChanges();
  return { form: fixture.componentInstance, fixture };
}

describe('Formulario de órdenes y proveedor del producto', () => {
  it('solo ofrece productos del proveedor y propone un precio editable', async () => {
    const { form, fixture } = await setup();
    form.form.controls.idProveedor.setValue(1);
    fixture.detectChanges();

    const options = [...fixture.nativeElement.querySelectorAll('#line-product-0 option')].map(
      (option: HTMLOptionElement) => option.textContent ?? '',
    );
    expect(options.join(' ')).toContain('CPU-12');
    expect(options.join(' ')).not.toContain('RAM-13');
    expect(options.join(' ')).not.toContain('SSD-14');

    const select = fixture.nativeElement.querySelector('#line-product-0') as HTMLSelectElement;
    select.value = [...select.options].find((option) =>
      option.textContent?.includes('CPU-12'),
    )!.value;
    select.dispatchEvent(new Event('change', { bubbles: true }));
    fixture.detectChanges();
    expect(form.lines.at(0).controls.idProducto.value).toBe(12);
    expect(form.lines.at(0).controls.precioUnitario.value).toBe('680.00');
    form.lines.at(0).controls.precioUnitario.setValue('620,00');
    expect(form.lineSubtotal(0)).toBe(620);
  });

  it('conserva la línea y su precio cuando cambia el proveedor, pero exige corregirla', async () => {
    const { form, fixture } = await setup();
    form.form.controls.idProveedor.setValue(1);
    form.lines.at(0).controls.idProducto.setValue(12);
    form.lines.at(0).controls.precioUnitario.setValue('620');

    form.form.controls.idProveedor.setValue(2);
    fixture.detectChanges();

    expect(form.lines.at(0).controls.idProducto.value).toBe(12);
    expect(form.lines.at(0).controls.precioUnitario.value).toBe('620');
    expect(form.productMismatch(12)).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('no está asignado al proveedor elegido');
    await form.submit();
    expect(form.error()).toContain('productos asignados a ese proveedor');
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
    expect((document.activeElement as HTMLElement).id).toBe('line-product-0');
  });

  it('al editar conserva el precio pactado aunque el catálogo tenga otro precio', async () => {
    const existing: OrdenResponse = {
      idOrden: 4,
      idProveedor: 1,
      nomProveedor: 'Proveedor Uno',
      idUsuario: 1,
      nomUsuario: 'Administrador',
      fechaEmision: '2026-09-30',
      fechaEstimadaEntrega: null,
      estado: 'Pendiente',
      detalles: [
        {
          idDetalle: 9,
          idProducto: 12,
          sku: 'CPU-12',
          nomProducto: 'Procesador',
          cantidad: 1,
          cantidadRecibida: 0,
          cantidadPorRecibir: 1,
          precioUnitario: 620,
          subtotal: 620,
        },
      ],
      totalOrden: 620,
    };
    const { form } = await setup(existing);

    expect(form.lines.at(0).controls.precioUnitario.value).toBe('620');
    expect(form.form.pristine).toBe(true);
  });
});
