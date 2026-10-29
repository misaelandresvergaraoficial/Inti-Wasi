import { AbstractControl } from '@angular/forms';
export function fieldError(control: AbstractControl, label: string): string {
  if (control.hasError('server')) return control.getError('server');
  if (control.hasError('required') || control.hasError('blank')) return `Ingresa ${label}.`;
  if (control.hasError('email')) return 'Ingresa un correo válido, por ejemplo rol.nombre@intiwasi.pe.';
  if (control.hasError('minlength'))
    return `Usa al menos ${control.getError('minlength').requiredLength} caracteres.`;
  if (control.hasError('maxlength'))
    return `Usa como máximo ${control.getError('maxlength').requiredLength} caracteres.`;
  if (control.hasError('emoji')) return `No uses emojis en ${label}.`;
  if (control.hasError('pattern'))
    return `Revisa ${label}: contiene caracteres no permitidos.`;
  return '';
}
export function focusFirstInvalid(form: HTMLElement): void {
  requestAnimationFrame(() =>
    form.querySelector<HTMLElement>(
      'input.ng-invalid, select.ng-invalid, textarea.ng-invalid',
    )?.focus(),
  );
}

// Misma cobertura que Patrones.SIN_EMOJIS del backend. Rangos como puntos
// de código para no depender de escapes en el fuente.
const RANGOS_EMOJI: Array<[number, number]> = [
  [0x2300, 0x23ff],
  [0x25a0, 0x25ff],
  [0x2600, 0x27bf],
  [0x2b00, 0x2bff],
  [0x3030, 0x3030],
  [0x303d, 0x303d],
  [0x3297, 0x3297],
  [0x3299, 0x3299],
  [0x200d, 0x200d],
  [0x20e3, 0x20e3],
  [0xfe0f, 0xfe0f],
];

const BMP_EMOJI = new RegExp(
  '[' +
    RANGOS_EMOJI.map(([a, b]) =>
      String.fromCharCode(a) + '-' + String.fromCharCode(b),
    ).join('') +
    ']',
);

function tieneAstral(valor: string): boolean {
  for (const ch of valor) {
    if ((ch.codePointAt(0) ?? 0) > 0xffff) return true;
  }
  return false;
}

export const sinEmojis = (control: AbstractControl) =>
  typeof control.value === 'string' &&
  (BMP_EMOJI.test(control.value) || tieneAstral(control.value))
    ? { emoji: true }
    : null;
