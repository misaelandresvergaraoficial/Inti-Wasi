export function productoIdDelError(error: unknown): number | null {
  const message = error instanceof Error ? error.message : '';
  const match = /producto:\s*(\d+)/i.exec(message);
  return match ? Number(match[1]) : null;
}
