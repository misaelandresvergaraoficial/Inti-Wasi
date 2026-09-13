package com.intiwasi.backend.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Usuarios")
public class Usuario {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "IdUsuario")
    private Integer idUsuario;
    @Column(name = "NomUsuario", nullable = false)
    private String nomUsuario;

    @Column(name = "Correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "Contrasena", nullable = false)
    private String contrasena;

    
    @Column(name = "Rol", nullable = false)
    private String rol;

    @Column(name = "Telefono")
    private String telefono;

    @Column(name = "Estado", nullable = false)
    private Integer estado = 1;
    @Column (name = "FechaRegistro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
