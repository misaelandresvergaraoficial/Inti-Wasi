package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.DashboardResponse;
import com.intiwasi.backend.dto.inventario.StockBajoResponse;
import com.intiwasi.backend.entity.StockBajo;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ProductoRepository productoRepository;
    private final DocumentoRepository documentoRepository;
    private final StockBajoRepository stockBajoRepository;

    @Transactional(readOnly = true)
    public DashboardResponse resumen() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        return DashboardResponse.builder()
                .totalProductosActivos(productoRepository.countByEstado((byte) 1))
                .productosConStockBajo(productoRepository.countConStockBajo((byte) 1))
                .entradasDelDia(documentoRepository.countByTipoDocumentoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThan(TipoDocumento.ENTRADA, inicio, fin))
                .salidasDelDia(documentoRepository.countByTipoDocumentoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThan(TipoDocumento.SALIDA, inicio, fin))
                .productosPorReponer(stockBajoRepository.findTop20ByOrderByUnidadesPorReponerDesc().stream().map(this::convertir).toList())
                .build();
    }

    private StockBajoResponse convertir(StockBajo s) {
        return StockBajoResponse.builder().idProducto(s.getIdProducto()).sku(s.getSku())
                .nomProducto(s.getNomProducto()).nomCategoria(s.getNomCategoria())
                .nomProveedor(s.getNomProveedor()).stockActual(s.getStockActual())
                .stockMinimo(s.getStockMinimo()).unidadesPorReponer(s.getUnidadesPorReponer()).build();
    }
}
