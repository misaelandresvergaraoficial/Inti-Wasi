import { promises as fs } from 'node:fs';
import path from 'node:path';
import type { Page } from '@playwright/test';

export interface DatosEvidencia {
  /** Identificador del caso, por ejemplo `CP-01`. */
  id: string;
  /** Requisito funcional asociado, por ejemplo `RF01`. */
  rf: string;
  /** Título corto del caso. */
  titulo: string;
  /** Resultado observado en la ejecución. */
  resultado: 'aprobado' | 'fallido';
}

interface LineaManifiesto extends DatosEvidencia {
  archivo: string;
  fecha: string;
}

const RAIZ_EVIDENCIAS = path.resolve(__dirname, '../../../docs/evidencias/e2e');

function fechaDeHoy(): string {
  return new Date().toISOString().slice(0, 10);
}

function slug(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 48);
}

/**
 * Deja una captura trazable de un caso de prueba.
 *
 * 1. Inyecta un rótulo visible con `CP-xx · RFxx · título`.
 * 2. Guarda la captura completa en `docs/evidencias/e2e/<fecha>/`.
 * 3. Añade la línea al `manifest.json` de esa fecha.
 */
export async function evidencia(page: Page, datos: DatosEvidencia): Promise<string> {
  const fecha = fechaDeHoy();
  const carpeta = path.join(RAIZ_EVIDENCIAS, fecha);
  await fs.mkdir(carpeta, { recursive: true });

  const etiqueta = `${datos.id} · ${datos.rf} · ${datos.titulo}`;
  await page.evaluate((texto) => {
    let rotulo = document.getElementById('iw-evidencia');
    if (!rotulo) {
      rotulo = document.createElement('div');
      rotulo.id = 'iw-evidencia';
      document.body.appendChild(rotulo);
    }
    rotulo.textContent = texto;
    rotulo.setAttribute(
      'style',
      [
        'position:fixed',
        'left:12px',
        'bottom:12px',
        'z-index:2147483647',
        'max-width:92vw',
        'padding:6px 10px',
        'border-radius:6px',
        'background:#111827',
        'color:#ffffff',
        'font:600 13px/1.4 system-ui,sans-serif',
        'pointer-events:none',
      ].join(';'),
    );
  }, etiqueta);

  const archivo = `${datos.rf}_${datos.id}_${slug(datos.titulo)}.png`;
  await page.screenshot({ path: path.join(carpeta, archivo), fullPage: true });

  await page.evaluate(() => document.getElementById('iw-evidencia')?.remove());

  const manifiesto = path.join(carpeta, 'manifest.json');
  let lineas: LineaManifiesto[] = [];
  try {
    lineas = JSON.parse(await fs.readFile(manifiesto, 'utf8')) as LineaManifiesto[];
  } catch {
    lineas = [];
  }

  lineas = lineas.filter((linea) => linea.id !== datos.id);
  lineas.push({
    ...datos,
    archivo: `${fecha}/${archivo}`,
    fecha,
  });
  await fs.writeFile(manifiesto, `${JSON.stringify(lineas, null, 2)}\n`, 'utf8');

  return path.join(carpeta, archivo);
}
