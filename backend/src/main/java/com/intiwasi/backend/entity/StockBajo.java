package com.intiwasi.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "VW_StockBajo")
@Immutable
@Getter
public class StockBajo {
    @Id @Column(name = "IdProducto") private Integer idProducto;
    @Column(name = "SKU") private String sku;
    @Column(name = "NomProducto") private String nomProducto;
    @Column(name = "NomCategoria") private String nomCategoria;
    @Column(name = "NomProveedor") private String nomProveedor;
    @Column(name = "StockActual") private Integer stockActual;
    @Column(name = "StockMinimo") private Integer stockMinimo;
    @Column(name = "UnidadesPorReponer") private Integer unidadesPorReponer;
}
