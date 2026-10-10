package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.DashboardResponse;
import com.intiwasi.backend.dto.inventario.StockBajoResponse;
import com.intiwasi.backend.dto.inventario.TendenciaMovimientoResponse;
import com.intiwasi.backend.entity.StockBajo;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.OrdenCompraRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ProductoRepository productoRepository;
    private final DocumentoRepository documentoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final StockBajoRepository stockBajoRepository;

    @Transactional(readOnly = true)
    public DashboardResponse resumen() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        return DashboardResponse.builder()
                .totalProductosActivos(productoRepository.countByEstado((byte) 1))
                .productosConStockBajo(productoRepository.countConStockBajo((byte) 1))
                .ordenesPorCompletar(ordenCompraRepository.countByEstadoIn(List.of("Pendiente", "Parcial")))
                .entradasDelDia(documentoRepository.countByTipoDocumentoAndEstadoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThan(TipoDocumento.ENTRADA, (byte) 1, inicio, fin))
                .salidasDelDia(documentoRepository.countByTipoDocumentoAndEstadoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThan(TipoDocumento.SALIDA, (byte) 1, inicio, fin))
                .productosPorReponer(stockBajoRepository.findTop20ByOrderByUnidadesPorReponerDesc().stream().map(this::convertir).toList())
                .movimientosUltimos30Dias(tendencia(inicio.toLocalDate()))
                .build();
    }

    private List<TendenciaMovimientoResponse> tendencia(LocalDate hoy) {
        LocalDate inicio = hoy.minusDays(29);
        Map<LocalDate, long[]> conteos = new HashMap<>();
        for (Object[] fila : documentoRepository.contarActividad(inicio.atStartOfDay(), hoy.plusDays(1).atStartOfDay())) {
            LocalDate fecha = LocalDate.parse(fila[0].toString());
            long[] cantidades = conteos.computeIfAbsent(fecha, ignorado -> new long[2]);
            cantidades["Entrada".equals(fila[1].toString()) ? 0 : 1] = ((Number) fila[2]).longValue();
        }
        return inicio.datesUntil(hoy.plusDays(1))
                .map(fecha -> {
                    long[] cantidades = conteos.getOrDefault(fecha, new long[2]);
                    return new TendenciaMovimientoResponse(fecha, cantidades[0], cantidades[1]);
                }).toList();
    }

    private StockBajoResponse convertir(StockBajo s) {
        return StockBajoResponse.builder().idProducto(s.getIdProducto()).sku(s.getSku())
                .nomProducto(s.getNomProducto()).nomCategoria(s.getNomCategoria())
                .nomProveedor(s.getNomProveedor()).stockActual(s.getStockActual())
                .stockMinimo(s.getStockMinimo()).unidadesPorReponer(s.getUnidadesPorReponer()).build();
    }
}
