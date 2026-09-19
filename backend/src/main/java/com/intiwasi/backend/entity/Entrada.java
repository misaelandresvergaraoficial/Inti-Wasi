package com.intiwasi.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Entradas")
@Getter
@Setter
@NoArgsConstructor
public class Entrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdEntrada")
    private Integer idEntrada;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdDocumento", nullable = false, unique = true)
    private Documento documento;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdOrden", nullable = false, unique = true)
    private OrdenCompra ordenCompra;

    @Column(name = "DocumentoRef", nullable = false, length = 50)
    private String documentoRef;

    @Column(name = "Observaciones", length = 255)
    private String observaciones;
}
