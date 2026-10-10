package com.intiwasi.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportePdfWriterTest {
    @Test
    void pdfMultipaginaConAcentosYFilasLargas() throws IOException {
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < 160; i++) {
            filas.add(List.of("SKU-" + i, "Procesador de prueba con descripción extensa " + i,
                    "Categoría electrónica", "Proveedor", "680.00", "10", "4", "Normal"));
        }

        byte[] contenido = new ReportePdfWriter().escribir("inventario",
                List.of("SKU", "Producto", "Categoría", "Proveedor", "Precio referencial", "Stock", "Mínimo", "Estado"), filas);

        try (var documento = Loader.loadPDF(contenido)) {
            assertTrue(documento.getNumberOfPages() > 1);
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Categoría electrónica"));
            assertTrue(texto.contains("SKU-159"));
            assertTrue(texto.contains("680.00"));
            assertTrue(texto.contains("Estado"));
            assertEquals(documento.getNumberOfPages(), texto.split("Inventario actual", -1).length - 1);
        }
    }
}
