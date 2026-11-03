import { describe, expect, it } from 'vitest';
import { formatBusinessDate } from './business-time';

describe('Fechas de negocio America/Lima', () => {
  it('muestra fechas civiles de órdenes sin inventar una hora ni cambiar de día', () => {
    const expected = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeZone: 'America/Lima' })
      .format(new Date('2026-11-02T00:00:00-05:00'));
    expect(formatBusinessDate('2026-11-02')).toBe(expected);
  });
  it('interpreta fecha local de negocio y timestamp UTC equivalente con el mismo resultado', () => {
    expect(formatBusinessDate('2026-11-01T23:30:00')).toBe(
      formatBusinessDate('2026-11-02T04:30:00Z'),
    );
    expect(formatBusinessDate('2026-11-01T23:30:00')).not.toBe(
      formatBusinessDate('2026-11-01T23:30:00Z'),
    );
  });
});
