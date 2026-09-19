package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Documento;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface DocumentoRepository extends Repository<Documento, Integer> {
    Documento saveAndFlush(Documento documento);
    Optional<Documento> findById(Integer id);
    long countByTipoDocumentoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThan(
            com.intiwasi.backend.entity.enums.TipoDocumento tipo,
            java.time.LocalDateTime inicio,
            java.time.LocalDateTime fin);
}
