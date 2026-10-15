export type OrdenEmision = 'asc' | 'desc';

export function enRangoDeEmision(fecha: string, desde: string, hasta: string): boolean {
  const dia = fecha.slice(0, 10);
  return (!desde || dia >= desde) && (!hasta || dia <= hasta);
}

export function ordenarPorEmision<T extends { fechaEmision: string }>(
  registros: T[],
  orden: OrdenEmision,
  id: (registro: T) => number,
): T[] {
  const factor = orden === 'asc' ? 1 : -1;
  return [...registros].sort(
    (a, b) => factor * (a.fechaEmision.localeCompare(b.fechaEmision) || id(a) - id(b)),
  );
}
