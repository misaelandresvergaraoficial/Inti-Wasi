import { AbstractControl } from '@angular/forms';
export function fieldError(control: AbstractControl, label: string): string {
  if (control.hasError('server')) return control.getError('server');
  if (control.hasError('required') || control.hasError('blank')) return `Ingresa ${label}.`;
  if (control.hasError('email')) return 'Ingresa un correo válido, por ejemplo nombre@empresa.com.';
  if (control.hasError('minlength'))
    return `Usa al menos ${control.getError('minlength').requiredLength} caracteres.`;
  if (control.hasError('maxlength'))
    return `Usa como máximo ${control.getError('maxlength').requiredLength} caracteres.`;
  return '';
}
export function focusFirstInvalid(form: HTMLElement): void {
  requestAnimationFrame(() =>
    form.querySelector<HTMLElement>('input.ng-invalid, select.ng-invalid')?.focus(),
  );
}
