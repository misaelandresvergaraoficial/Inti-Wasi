package com.intiwasi.backend.dto.inventario;

import java.time.LocalDate;

public record TendenciaMovimientoResponse(LocalDate fecha, long entradas, long salidas) {}
