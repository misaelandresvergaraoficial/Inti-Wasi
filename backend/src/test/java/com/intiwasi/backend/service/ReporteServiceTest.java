package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.StockBajo;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {
    @Mock ProductoRepository productos;
    @Mock StockBajoRepository stockBajo;
    @Mock MovimientoConsultaService movimientos;
    @Mock ReportePdfWriter pdf;
    @InjectMocks ReporteService service;

    @Test
    void noGeneraArchivoCuandoNoHayResultados() {
        when(stockBajo.buscar(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertTrue(service.exportar("reposicion", "excel", null, null, null, null, null).isEmpty());
    }

    @Test
    void rechazaExportacionSuperiorAlLimiteAntesDeGenerarArchivo() {
        var fila = new StockBajo();
        when(stockBajo.buscar(isNull(), any(Pageable.class))).thenAnswer(call -> {
            Pageable page = call.getArgument(1);
            return new PageImpl<>(List.of(fila), page, 10_001);
        });

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.exportar("reposicion", "excel", null, null, null, null, null));
        assertTrue(error.getMessage().contains("10 000"));
    }

    @Test
    void exportaTodasLasPaginasSinCortarEnDoscientos() {
        List<StockBajo> filas = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            var fila = new StockBajo();
            ReflectionTestUtils.setField(fila, "idProducto", i + 1);
            ReflectionTestUtils.setField(fila, "sku", "SKU-" + i);
            ReflectionTestUtils.setField(fila, "nomProducto", "Producto " + i);
            ReflectionTestUtils.setField(fila, "nomCategoria", "Categoría");
            ReflectionTestUtils.setField(fila, "stockActual", 1);
            ReflectionTestUtils.setField(fila, "stockMinimo", 2);
            ReflectionTestUtils.setField(fila, "unidadesPorReponer", 1);
            filas.add(fila);
        }
        when(stockBajo.buscar(isNull(), any(Pageable.class))).thenAnswer(call -> {
            Pageable page = call.getArgument(1);
            int inicio = (int) page.getOffset();
            int fin = Math.min(inicio + page.getPageSize(), filas.size());
            return new PageImpl<>(filas.subList(inicio, fin), page, filas.size());
        });

        String xml = new String(service.exportar("reposicion", "excel", null, null, null, null, null)
                .orElseThrow(), StandardCharsets.UTF_8);

        assertTrue(xml.contains("SKU-0"));
        assertTrue(xml.contains("SKU-200"));
        assertEquals(202, xml.split("<Row>", -1).length - 1);
    }
}
