package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface MovimientoInventarioRepository extends Repository<MovimientoInventario, Integer> {
    MovimientoInventario save(MovimientoInventario movimiento);
    List<MovimientoInventario> saveAll(Iterable<MovimientoInventario> movimientos);
    void flush();

    @EntityGraph(attributePaths = {"producto"})
    List<MovimientoInventario> findByDocumento_IdDocumentoOrderByIdMovimientoAsc(Integer idDocumento);
}
