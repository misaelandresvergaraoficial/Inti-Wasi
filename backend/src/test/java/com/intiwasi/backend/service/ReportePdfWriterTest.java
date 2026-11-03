package com.intiwasi.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportePdfWriterTest {
    private final ReportePdfWriter writer = new ReportePdfWriter();
    private final List<String> encabezados = List.of("SKU", "Producto", "Categoría", "Proveedor", "Precio referencial", "Stock", "Mínimo", "Estado");

    @Test
    void conservaDiezMilFilasSinTruncarElReporte() throws Exception {
        List<List<String>> filas = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            filas.add(List.of("QA-" + String.format("%05d", i), "Producto Ensayo Ñandú " + i,
                    "Categoría de prueba", "Proveedor de prueba S.A.C.", "25.50", "1", "2", "Stock bajo"));
        }
        long inicio = System.nanoTime();
        byte[] contenido = writer.escribir("inventario", encabezados, filas, "2026-11-02 14:00:00 -05:00");
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        Path carpeta = Path.of("target/reportes-pdf-prueba");
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve("inventario-10000.pdf"), contenido);
        Files.writeString(carpeta.resolve("generacion-ms.txt"), Long.toString(milisegundos));
        try (var documento = Loader.loadPDF(contenido)) {
            String texto = new PDFTextStripper().getText(documento);
            var identificadores = Pattern.compile("QA-\\d{5}").matcher(texto).results().map(resultado -> resultado.group()).toList();
            assertEquals(10000, identificadores.size());
            assertEquals(10000, identificadores.stream().distinct().count());
            assertTrue(texto.contains("QA-00001"));
            assertTrue(texto.contains("QA-10000"));
            assertTrue(documento.getNumberOfPages() > 1);
            ImageIO.write(new PDFRenderer(documento).renderImageWithDPI(0, 96), "png", carpeta.resolve("primera-pagina.png").toFile());
        }
        System.out.println("PDF_GENERACION_10000_MS=" + milisegundos);
    }

    @Test
    void conservaTildesYTextosLargosAlDividirCeldas() throws Exception {
        String sku = "CODIGO-EXTENSO-QUE-DEBE-CONSERVARSE-COMPLETO-0123456789";
        String nombre = "Oficina: Árbol Ñandú, útil para almacén y distribución";
        byte[] contenido = writer.escribir("inventario", encabezados,
                List.of(List.of(sku, nombre, "Categoría", "José Muñoz", "12.50", "1", "2", "Stock bajo")),
                "2026-11-02 14:00:00 -05:00");
        try (var documento = Loader.loadPDF(contenido)) {
            String texto = new PDFTextStripper().getText(documento).replaceAll("\\s+", "");
            assertTrue(texto.contains(sku));
            assertTrue(texto.contains(nombre.replaceAll("\\s+", "")));
            assertTrue(texto.contains("JoséMuñoz"));
            assertTrue(texto.contains("America/Lima"));
        }
    }

    @Test
    void conservaTextoDeOtrasEscrituras() throws Exception {
        byte[] contenido = writer.escribir("inventario", encabezados,
                List.of(List.of("SKU-1", "Producto Ωμέγα", "Categoría", "Proveedor", "12.50", "1", "2", "Stock bajo")),
                "2026-11-02 14:00:00 -05:00");
        try (var documento = Loader.loadPDF(contenido)) {
            assertTrue(new PDFTextStripper().getText(documento).contains("Ωμέγα"));
        }
    }
}
