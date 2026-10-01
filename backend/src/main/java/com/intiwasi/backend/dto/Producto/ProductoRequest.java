package com.intiwasi.backend.dto.Producto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data 
public class ProductoRequest {
    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 30, message = "El SKU no debe exceder los 30 caracteres")
    private String sku;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre no debe exceder los 150 caracteres")
    private String nomProducto;

    @NotNull(message = "La categoría es obligatoria")
    private Integer idCategoria;

    private Integer idProveedor;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio debe ser mayor o igual a cero")
    private BigDecimal precio;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;
}
