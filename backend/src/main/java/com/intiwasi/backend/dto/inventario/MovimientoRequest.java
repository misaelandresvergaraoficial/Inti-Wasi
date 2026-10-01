package com.intiwasi.backend.dto.inventario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MovimientoRequest {
    @NotNull(message = "El producto es obligatorio")
    private Integer idProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser entera y mayor que cero")
    private Integer cantidad;
}
