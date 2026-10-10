package com.intiwasi.backend.dto.inventario;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalProductosActivos;
    private long productosConStockBajo;
    private long ordenesPorCompletar;
    private long entradasDelDia;
    private long salidasDelDia;
    private List<StockBajoResponse> productosPorReponer;
    private List<TendenciaMovimientoResponse> movimientosUltimos30Dias;
}
