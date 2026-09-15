package com.intiwasi.backend.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "Productos")
@AllArgsConstructor  
@NoArgsConstructor
@Getter
@Setter
@Builder    
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProducto") 
    private Integer idProducto;

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 30, message = "El SKU no debe exceder los 30 caracteres")
    @Column(name = "SKU", nullable = false, unique = true, length = 30)
    private String sku;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre del producto no debe exceder los 150 caracteres")
    @Column(name = "NomProducto", nullable = false, length = 150)
    private String nomProducto;

    // -------------------------------------------------------------
    // LLAVE FORÁNEA 1: Categoría (NOT NULL)
    // -------------------------------------------------------------
    @NotNull(message = "La categoría es obligatoria")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "IdCategoria", nullable = false)
    private Categoria categoria;

    // -------------------------------------------------------------
    // LLAVE FORÁNEA 2: Proveedor (NULL en el DDL)
    // -------------------------------------------------------------
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "IdProveedor", nullable = true)
    private Proveedor proveedor;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Column(name = "Precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo debe ser mayor o igual a cero")
    @Column(name = "StockMinimo", nullable = false)
    private Integer stockMinimo;

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock actual debe ser mayor o igual a cero")
    @Column(name = "StockActual", nullable = false)
    private Integer stockActual;

    @Column(name = "Estado", nullable = false)
    private Byte estado; // 1: Activo, 0: Inactivo (Borrado lógico)

}
