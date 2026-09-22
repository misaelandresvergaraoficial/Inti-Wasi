package com.intiwasi.backend.entity;

import com.intiwasi.backend.entity.converter.TipoDocumentoConverter;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Table(name = "VW_Kardex")
@Immutable
@Getter
public class KardexMovimiento {
    @Id @Column(name = "IdMovimiento") private Integer idMovimiento;
    @Column(name = "IdDocumento") private Integer idDocumento;
    @Convert(converter = TipoDocumentoConverter.class)
    @Column(name = "TipoDocumento") private TipoDocumento tipoDocumento;
    @Column(name = "FechaEmision") private LocalDateTime fechaEmision;
    @Column(name = "IdUsuario") private Integer idUsuario;
    @Column(name = "UsuarioResponsable") private String usuarioResponsable;
    @Column(name = "IdProducto") private Integer idProducto;
    @Column(name = "SKU") private String sku;
    @Column(name = "NomProducto") private String nomProducto;
    @Column(name = "NomCategoria") private String nomCategoria;
    @Column(name = "Cantidad") private Integer cantidad;
    @Column(name = "CantidadConSigno") private Integer cantidadConSigno;
    @Column(name = "Motivo") private String motivo;
    @Column(name = "IdOrden") private Integer idOrden;
    @Column(name = "DocumentoRef") private String documentoRef;
    @Column(name = "EstadoDocumento") private Byte estadoDocumento;
}
