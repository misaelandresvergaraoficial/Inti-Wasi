package com.intiwasi.backend.service;

import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.OrdenCompraRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock ProductoRepository productos;
    @Mock DocumentoRepository documentos;
    @Mock OrdenCompraRepository ordenes;
    @Mock StockBajoRepository stockBajo;
    @InjectMocks DashboardService service;

    @Test
    void completaTreintaDiasYSeparaEntradasDeSalidas() {
        LocalDate hoy = LocalDate.now();
        when(stockBajo.findTop20ByOrderByUnidadesPorReponerDesc()).thenReturn(List.of());
        when(ordenes.countByEstadoIn(List.of("Pendiente", "Parcial"))).thenReturn(3L);
        when(documentos.contarActividad(any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{hoy.toString(), "Entrada", 2L},
                new Object[]{hoy.toString(), "Salida", 1L}));

        var resumen = service.resumen();

        assertEquals(3, resumen.getOrdenesPorCompletar());
        assertEquals(30, resumen.getMovimientosUltimos30Dias().size());
        assertEquals(hoy.minusDays(29), resumen.getMovimientosUltimos30Dias().getFirst().fecha());
        assertEquals(0, resumen.getMovimientosUltimos30Dias().getFirst().entradas());
        assertEquals(2, resumen.getMovimientosUltimos30Dias().getLast().entradas());
        assertEquals(1, resumen.getMovimientosUltimos30Dias().getLast().salidas());
    }
}
