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
                    "Categoría electrónica", "Proveedor", "10", "4"));
        }

        byte[] contenido = new ReportePdfWriter().escribir("inventario",
                List.of("SKU", "Producto", "Categoría", "Proveedor", "Stock", "Mínimo"), filas);

        try (var documento = Loader.loadPDF(contenido)) {
            assertTrue(documento.getNumberOfPages() > 1);
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Categoría electrónica"));
            assertTrue(texto.contains("SKU-159"));
            assertEquals(documento.getNumberOfPages(), texto.split("Inventario actual", -1).length - 1);
        }
    }
}
