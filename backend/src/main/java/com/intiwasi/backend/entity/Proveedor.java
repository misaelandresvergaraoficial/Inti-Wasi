package com.intiwasi.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Entity
@Table(name = "Proveedores")
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdProveedor")
    private Integer idProveedor;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Column(name = "NomProveedor", length = 100, nullable = false)
    private String nomProveedor;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener 11 dígitos numéricos exactos")
    @Column(name = "RUC", length = 11, nullable = false, unique = true)
    private String ruc;

    @Column(name = "Contacto", length = 100)
    private String contacto;

    @Column(name = "Telefono", length = 20)
    private String telefono;

    @Column(name = "Direccion", length = 150)
    private String direccion;

    @Column(name = "Estado", nullable = false)
    private Integer estado = 1;
}
