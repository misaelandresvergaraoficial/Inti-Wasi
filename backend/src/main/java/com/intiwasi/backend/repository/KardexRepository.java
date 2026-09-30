package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.KardexMovimiento;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface KardexRepository extends Repository<KardexMovimiento, Integer> {
    Optional<KardexMovimiento> findById(Integer id);

    @Query("""
            select k from KardexMovimiento k
            where (:inicio is null or k.fechaEmision >= :inicio)
              and (:fin is null or k.fechaEmision < :fin)
              and (:idProducto is null or k.idProducto = :idProducto)
              and (:tipo is null or k.tipoDocumento = :tipo)
              and (:idUsuario is null or k.idUsuario = :idUsuario)
            """)
    Page<KardexMovimiento> buscar(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("idProducto") Integer idProducto,
            @Param("tipo") TipoDocumento tipo,
            @Param("idUsuario") Integer idUsuario,
            Pageable pageable);
}
