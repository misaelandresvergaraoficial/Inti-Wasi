export function formatBusinessDate(value: string, dateStyle: 'medium' | 'long' = 'medium'): string {
  const dateOnly = /^\d{4}-\d{2}-\d{2}$/.test(value);
  const dated = dateOnly
    ? `${value}T00:00:00-05:00`
    : /(?:Z|[+-]\d{2}:?\d{2})$/.test(value)
      ? value
      : `${value}-05:00`;
  return new Intl.DateTimeFormat('es-PE', {
    dateStyle,
    timeStyle: dateOnly ? undefined : 'short',
    timeZone: 'America/Lima',
  }).format(new Date(dated));
}
