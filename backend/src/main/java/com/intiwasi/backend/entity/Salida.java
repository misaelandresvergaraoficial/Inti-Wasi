package com.intiwasi.backend.entity;

import com.intiwasi.backend.entity.converter.MotivoSalidaConverter;
import com.intiwasi.backend.entity.enums.MotivoSalida;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "Salidas")
@Getter
@Setter
@NoArgsConstructor
public class Salida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdSalida")
    private Integer idSalida;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdDocumento", nullable = false, unique = true)
    private Documento documento;

    @Convert(converter = MotivoSalidaConverter.class)
    @Column(name = "Motivo", nullable = false, length = 14)
    private MotivoSalida motivo;

    @Column(name = "Observaciones", length = 255)
    private String observaciones;
}
