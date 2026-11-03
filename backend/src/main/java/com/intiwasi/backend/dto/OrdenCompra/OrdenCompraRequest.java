package com.intiwasi.backend.dto.OrdenCompra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrdenCompraRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private Integer idProveedor;

    private LocalDate fechaEstimadaEntrega;

    @Valid
    @NotEmpty(message = "La orden debe contener al menos un producto")
    @Size(max = 100, message = "La orden admite hasta 100 productos")
    private List<DetalleOrdenCompraRequest> detalles;
}
