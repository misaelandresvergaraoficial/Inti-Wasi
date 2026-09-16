package com.intiwasi.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity 
@Table (name = "Categorias")
public class Categoria {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "IdCategoria")
    private Integer idCategoria;

    @Column (name = "NomCategoria", nullable = false, length = 100)
    private String nomCategoria;
    
    @Column (name = "Estado", nullable = false)
    private Integer estado = 1;
}
