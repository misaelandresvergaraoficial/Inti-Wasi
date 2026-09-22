package com.intiwasi.backend.entity;

import com.intiwasi.backend.entity.converter.TipoDocumentoConverter;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "Documentos")
@Getter
@Setter
@NoArgsConstructor
@Immutable
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IdDocumento")
    private Integer idDocumento;

    @Convert(converter = TipoDocumentoConverter.class)
    @Column(name = "TipoDocumento", nullable = false, length = 7)
    private TipoDocumento tipoDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IdUsuario", nullable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(name = "FechaEmision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "Estado", nullable = false)
    private Byte estado = (byte) 1;
}
