package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {
    @Mock ProductoRepository productos;
    @Mock StockBajoRepository stockBajo;
    @Mock MovimientoConsultaService movimientos;
    @Mock ReportePdfWriter pdf;
    @InjectMocks ReporteService service;

    @Test
    void conservaOrdenEstableYPaginacionEnLosTresReportes() {
        when(productos.buscarInventario(isNull(), any(Pageable.class))).thenReturn(Page.empty());
        when(stockBajo.buscar(isNull(), any(Pageable.class))).thenReturn(Page.empty());
        when(movimientos.consultar(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());
        var pagina = PageRequest.of(2, 500);

        service.inventario(null, pagina);
        service.reposicion(null, pagina);
        service.movimientos(null, null, null, null, null, pagina);

        var paginas = ArgumentCaptor.forClass(Pageable.class);
        verify(productos).buscarInventario(isNull(), paginas.capture());
        verify(stockBajo).buscar(isNull(), paginas.capture());
        verify(movimientos).consultar(isNull(), isNull(), isNull(), isNull(), isNull(), paginas.capture());
        var ordenes = paginas.getAllValues().stream()
                .map(p -> p.getSort().stream().map(Sort.Order::getProperty).toList()).toList();
        assertEquals(List.of(List.of("idProducto"), List.of("idProducto"),
                List.of("fechaEmision", "idMovimiento")), ordenes);
        for (var solicitada : paginas.getAllValues()) {
            assertEquals(2, solicitada.getPageNumber());
            assertEquals(200, solicitada.getPageSize());
            assertTrue(solicitada.getSort().stream().allMatch(Sort.Order::isAscending));
        }
    }

    @Test
    void exportacionDeInventarioIncluyePrecioReferencial() {
        Producto producto = mock(Producto.class);
        Categoria categoria = mock(Categoria.class);
        when(producto.getSku()).thenReturn("-1+2");
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
        assertTrue(csv.contains("Generado (America/Lima)"));
        assertTrue(csv.contains("\"'-1+2\""));
    }
    @Test
    void rechazaExportacionMayorADiezMilAntesDeReunirPaginas() {
        when(productos.buscarInventario(isNull(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<Producto>(List.of(), call.getArgument(1), 10001));
        org.junit.jupiter.api.Assertions.assertThrows(com.intiwasi.backend.exception.ReglaNegocioException.class,
                () -> service.exportar("inventario", "csv", null, null, null, null, null));
        org.mockito.Mockito.verify(productos, org.mockito.Mockito.times(1)).buscarInventario(isNull(), any(Pageable.class));
    }

    @Test
    void noGeneraArchivoCuandoLaConsultaEstaVacia() {
        when(productos.buscarInventario(isNull(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<Producto>(List.of(), call.getArgument(1), 0));
        assertTrue(service.exportar("inventario", "csv", null, null, null, null, null).isEmpty());
    }

}
