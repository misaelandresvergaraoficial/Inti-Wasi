package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;

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
        assertTrue(csv.contains("\"680.00\""));
    }
}
