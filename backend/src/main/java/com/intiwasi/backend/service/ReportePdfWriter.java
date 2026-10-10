package com.intiwasi.backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReportePdfWriter {
    private static final float MARGEN = 32;
    private static final float TAMANO_TEXTO = 8;
    private static final float ALTO_LINEA = 11;
    private static final float ALTO_CABECERA = 25;
    private static final float ANCHO_TABLA = PDRectangle.A4.getHeight() - MARGEN * 2;

    public byte[] escribir(String reporte, List<String> encabezados, List<List<String>> filas) {
        try (PDDocument documento = new PDDocument();
             InputStream archivoFuente = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            if (archivoFuente == null) throw new IllegalStateException("No se encontró la fuente del reporte");
            PDFont fuente = PDType0Font.load(documento, archivoFuente);
            float[] anchos = anchos(encabezados);
            Pagina pagina = nuevaPagina(documento, fuente, titulo(reporte), encabezados, anchos, 1);

            for (List<String> fila : filas) {
                List<List<String>> celdas = new ArrayList<>();
                int lineas = 1;
                for (int i = 0; i < fila.size(); i++) {
                    List<String> contenido = dividir(fila.get(i), fuente, anchos[i] - 8);
                    celdas.add(contenido);
                    lineas = Math.max(lineas, contenido.size());
                }
                float alto = lineas * ALTO_LINEA + 8;
                if (pagina.y - alto < MARGEN) {
                    pagina.stream.close();
                    pagina = nuevaPagina(documento, fuente, titulo(reporte), encabezados, anchos,
                            documento.getNumberOfPages() + 1);
                }
                dibujarFila(pagina, celdas, anchos, alto, fuente);
            }
            pagina.stream.close();

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo generar el PDF del reporte", error);
        }
    }

    private Pagina nuevaPagina(PDDocument documento, PDFont fuente, String titulo,
                               List<String> encabezados, float[] anchos, int numero) throws IOException {
        PDPage hoja = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        documento.addPage(hoja);
        PDPageContentStream stream = new PDPageContentStream(documento, hoja);
        float y = hoja.getMediaBox().getHeight() - MARGEN;
        texto(stream, fuente, 15, MARGEN, y - 15, titulo);
        texto(stream, fuente, 8, MARGEN, MARGEN / 2, "Inti Wasi · Página " + numero);
        y -= 34;

        stream.setNonStrokingColor(new Color(235, 243, 243));
        stream.addRect(MARGEN, y - ALTO_CABECERA, ANCHO_TABLA, ALTO_CABECERA);
        stream.fill();
        float x = MARGEN;
        for (int i = 0; i < encabezados.size(); i++) {
            List<String> lineas = dividir(encabezados.get(i), fuente, anchos[i] - 8);
            for (int linea = 0; linea < lineas.size(); linea++) {
                texto(stream, fuente, TAMANO_TEXTO, x + 4,
                        y - (lineas.size() == 1 ? 16 : 10 + linea * ALTO_LINEA), lineas.get(linea));
            }
            x += anchos[i];
        }
        return new Pagina(stream, y - ALTO_CABECERA);
    }

    private void dibujarFila(Pagina pagina, List<List<String>> celdas, float[] anchos,
                             float alto, PDFont fuente) throws IOException {
        float x = MARGEN;
        for (int i = 0; i < celdas.size(); i++) {
            for (int linea = 0; linea < celdas.get(i).size(); linea++) {
                texto(pagina.stream, fuente, TAMANO_TEXTO, x + 4,
                        pagina.y - 12 - linea * ALTO_LINEA, celdas.get(i).get(linea));
            }
            x += anchos[i];
        }
        pagina.y -= alto;
        pagina.stream.setStrokingColor(new Color(224, 230, 233));
        pagina.stream.moveTo(MARGEN, pagina.y);
        pagina.stream.lineTo(MARGEN + ANCHO_TABLA, pagina.y);
        pagina.stream.stroke();
    }

    private void texto(PDPageContentStream stream, PDFont fuente, float tamano,
                       float x, float y, String valor) throws IOException {
        stream.setNonStrokingColor(new Color(32, 43, 51));
        stream.beginText();
        stream.setFont(fuente, tamano);
        stream.newLineAtOffset(x, y);
        stream.showText(valor);
        stream.endText();
    }

    private List<String> dividir(String valor, PDFont fuente, float ancho) throws IOException {
        String limpio = valor.replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) return List.of("");
        List<String> lineas = new ArrayList<>();
        StringBuilder linea = new StringBuilder();
        for (String palabra : limpio.split(" ")) {
            String candidata = linea.isEmpty() ? palabra : linea + " " + palabra;
            if (medir(fuente, candidata) <= ancho) {
                linea.setLength(0);
                linea.append(candidata);
                continue;
            }
            if (!linea.isEmpty()) {
                lineas.add(linea.toString());
                linea.setLength(0);
            }
            for (int i = 0; i < palabra.length(); i++) {
                String siguiente = linea.toString() + palabra.charAt(i);
                if (!linea.isEmpty() && medir(fuente, siguiente) > ancho) {
                    lineas.add(linea.toString());
                    linea.setLength(0);
                }
                linea.append(palabra.charAt(i));
            }
        }
        if (!linea.isEmpty()) lineas.add(linea.toString());
        return lineas;
    }

    private float medir(PDFont fuente, String valor) throws IOException {
        return fuente.getStringWidth(valor) * TAMANO_TEXTO / 1000;
    }

    private float[] anchos(List<String> encabezados) {
        float[] anchos = new float[encabezados.size()];
        float total = 0;
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = switch (encabezados.get(i)) {
                case "Producto" -> 3;
                case "Motivo", "Proveedor", "Categoría", "Usuario" -> 1.7f;
                case "Fecha" -> 1.8f;
                default -> 1.1f;
            };
            total += anchos[i];
        }
        for (int i = 0; i < anchos.length; i++) anchos[i] = ANCHO_TABLA * anchos[i] / total;
        return anchos;
    }

    private String titulo(String reporte) {
        return switch (reporte.toLowerCase()) {
            case "inventario" -> "Inventario actual";
            case "movimientos" -> "Historial de movimientos";
            case "reposicion" -> "Productos por reponer";
            default -> reporte;
        };
    }

    private static class Pagina {
        private final PDPageContentStream stream;
        private float y;

        private Pagina(PDPageContentStream stream, float y) {
            this.stream = stream;
            this.y = y;
        }
    }
}
