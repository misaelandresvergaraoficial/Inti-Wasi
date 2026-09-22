package com.intiwasi.backend.entity;

import com.intiwasi.backend.entity.converter.TipoAjusteConverter;
import com.intiwasi.backend.entity.enums.TipoAjuste;
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
@Table(name = "Ajustes")
@Getter
@Setter
@NoArgsConstructor
public class Ajuste {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdAjuste")
    private Integer idAjuste;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdDocumento", nullable = false, unique = true)
    private Documento documento;

    @Convert(converter = TipoAjusteConverter.class)
    @Column(name = "TipoAjuste", nullable = false, length = 10)
    private TipoAjuste tipoAjuste;

    @Column(name = "Motivo", nullable = false, length = 255)
    private String motivo;

    @Column(name = "Observaciones", length = 255)
    private String observaciones;
}
