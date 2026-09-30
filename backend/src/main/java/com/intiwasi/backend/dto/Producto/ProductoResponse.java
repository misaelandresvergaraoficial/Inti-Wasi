package com.intiwasi.backend.dto.Producto;

import java.math.BigDecimal;

import lombok.Data;

@Data 
public class ProductoResponse {

    private Integer idProducto;
    private String sku;
    private String nomProducto;

    private Integer idCategoria;
    private String nomCategoria;

    private Integer idProveedor;
    private String nomProveedor;

    private BigDecimal precio;
    private Integer stockMinimo;
    private Integer stockActual;
    private Byte estado;
}
