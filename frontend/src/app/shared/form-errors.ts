import { AbstractControl, ValidatorFn } from '@angular/forms';

export const normalizarNombre = (value: string): string => value.trim().normalize('NFC');

export function nombrePersonal(maxLength: number, optional = false): ValidatorFn {
  return (control) => {
    const value = normalizarNombre(String(control.value ?? ''));
    if (!value && optional) return null;
    const validCharacters = /^[\p{L}\p{M}]+(?:[ '’-][\p{L}\p{M}]+)*$/u.test(value);
    if (!validCharacters && tieneEmojis(value)) return { emoji: true };
    if (value.length < 2) return { minlength: { requiredLength: 2, actualLength: value.length } };
    if (value.length > maxLength)
      return { maxlength: { requiredLength: maxLength, actualLength: value.length } };
    return validCharacters ? null : { pattern: true };
  };
}
export function fieldError(control: AbstractControl, label: string): string {
  if (control.hasError('server')) return control.getError('server');
  if (control.hasError('required') || control.hasError('blank')) return `Ingresa ${label}.`;
  if (control.hasError('email'))
    return 'Ingresa un correo válido, por ejemplo rol.nombre@intiwasi.pe.';
  if (control.hasError('minlength'))
    return `Usa al menos ${control.getError('minlength').requiredLength} caracteres.`;
  if (control.hasError('maxlength'))
    return `Usa como máximo ${control.getError('maxlength').requiredLength} caracteres.`;
  if (control.hasError('emoji')) return `No uses emojis en ${label}.`;
  if (control.hasError('pattern')) return `Revisa ${label}: contiene caracteres no permitidos.`;
  return '';
}
export function focusFirstInvalid(form: HTMLElement): void {
  requestAnimationFrame(() =>
    form
      .querySelector<HTMLElement>('input.ng-invalid, select.ng-invalid, textarea.ng-invalid')
      ?.focus(),
  );
}

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
    RANGOS_EMOJI.map(([a, b]) => String.fromCharCode(a) + '-' + String.fromCharCode(b)).join('') +
    ']',
);

const EMOJI_ASTRAL = /[\u{1F000}-\u{1FAFF}\u{E0020}-\u{E007F}\p{Cs}]/u;

export const tieneEmojis = (value: string): boolean =>
  BMP_EMOJI.test(value) || EMOJI_ASTRAL.test(value);

export const sinEmojis = (control: AbstractControl) =>
  typeof control.value === 'string' && tieneEmojis(control.value) ? { emoji: true } : null;
