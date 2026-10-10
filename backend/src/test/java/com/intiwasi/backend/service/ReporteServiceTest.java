package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.StockBajo;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Categoria;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

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

        assertTrue(service.exportar("reposicion", "csv", null, null, null, null, null).isEmpty());
    }

    @Test
    void rechazaExportacionSuperiorAlLimiteAntesDeGenerarArchivo() {
        var fila = new StockBajo();
        when(stockBajo.buscar(isNull(), any(Pageable.class))).thenAnswer(call -> {
            Pageable page = call.getArgument(1);
            return new PageImpl<>(List.of(fila), page, 10_001);
        });

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.exportar("reposicion", "csv", null, null, null, null, null));
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

        String csv = new String(service.exportar("reposicion", "csv", null, null, null, null, null)
                .orElseThrow(), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF\"SKU\""));
        assertTrue(csv.contains("\"SKU-0\""));
        assertTrue(csv.contains("\"SKU-200\""));
        assertEquals(202, csv.split("\r\n", -1).length - 1);
    }

    @Test
    void exportacionDeInventarioIncluyePrecioReferencial() {
        Producto producto = mock(Producto.class);
        Categoria categoria = mock(Categoria.class);
        when(producto.getSku()).thenReturn("PRO-1");
        when(producto.getNomProducto()).thenReturn("Producto, 27\" de prueba");
        when(producto.getCategoria()).thenReturn(categoria);
        when(categoria.getNomCategoria()).thenReturn("Categoría");
        when(producto.getPrecio()).thenReturn(new BigDecimal("680.00"));
        when(producto.getStockActual()).thenReturn(5);
        when(producto.getStockMinimo()).thenReturn(2);
        when(productos.buscarInventario(isNull(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(producto), call.getArgument(1), 1));

        String csv = new String(service.exportar("inventario", "csv", null, null, null, null, null)
                .orElseThrow(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Precio referencial\""));
        assertTrue(csv.contains("\"Producto, 27\"\" de prueba\""));
        assertTrue(csv.contains("\"680.00\""));
        assertTrue(csv.contains("\"Estado\""));
        assertTrue(csv.contains("\"Normal\""));
        assertThrows(IllegalArgumentException.class,
                () -> service.exportar("inventario", "excel", null, null, null, null, null));
    }
}
