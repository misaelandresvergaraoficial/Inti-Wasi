package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Documento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentoRepository extends JpaRepository<Documento, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"usuario", "documentoOrigen"})
    @Query("select d from Documento d where d.idDocumento = :id")
    Optional<Documento> findByIdForUpdate(@Param("id") Integer id);

    @EntityGraph(attributePaths = {"usuario", "documentoOrigen"})
    @Query("select d from Documento d order by d.idDocumento desc")
    List<Documento> findAllConRelaciones();

    @EntityGraph(attributePaths = {"usuario", "documentoOrigen"})
    @Query("select d from Documento d where d.idDocumento = :id")
    Optional<Documento> findByIdConRelaciones(@Param("id") Integer id);

    long countByTipoDocumentoAndEstadoAndFechaEmisionGreaterThanEqualAndFechaEmisionLessThan(
            com.intiwasi.backend.entity.enums.TipoDocumento tipo,
            Byte estado,
            java.time.LocalDateTime inicio,
            java.time.LocalDateTime fin);

    @Query(value = "SELECT DATE(FechaEmision), TipoDocumento, COUNT(*) FROM Documentos " +
            "WHERE Estado = 1 AND TipoDocumento IN ('Entrada', 'Salida') " +
            "AND FechaEmision >= :inicio AND FechaEmision < :fin " +
            "GROUP BY DATE(FechaEmision), TipoDocumento", nativeQuery = true)
    List<Object[]> contarActividad(@Param("inicio") java.time.LocalDateTime inicio,
                                    @Param("fin") java.time.LocalDateTime fin);
}
