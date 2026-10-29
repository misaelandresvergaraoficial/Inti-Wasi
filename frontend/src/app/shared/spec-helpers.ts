import { EnvironmentProviders, Provider, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { NoticeService } from '../core/notice.service';

function baseProviders(extra: Array<Provider | EnvironmentProviders>): Array<Provider | EnvironmentProviders> {
  return [
    provideRouter([]),
    { provide: AuthService, useValue: { valid: () => true } },
    { provide: NoticeService, useValue: {} },
    { provide: MatDialog, useValue: {} },
    {
      provide: ActivatedRoute,
      useValue: { snapshot: { paramMap: convertToParamMap({}) } },
    },
    ...extra,
  ];
}

/**
 * Prepara un formulario standalone con sus dependencias base burladas,
 * ejecuta la detección de cambios inicial y devuelve su `form`.
 */
export async function setupForm(
  component: Type<unknown>,
  extra: Array<Provider | EnvironmentProviders> = [],
): Promise<any> {
  await TestBed.configureTestingModule({
    imports: [component],
    providers: baseProviders(extra),
  }).compileComponents();

  const fixture = TestBed.createComponent(component);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return (fixture.componentInstance as { form: unknown }).form;
}
