package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Proveedor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    List<Proveedor> findByEstado(Byte estado);

    boolean existsByRuc(String ruc);

    boolean existsByNomProveedor(String nomProveedor);

    boolean existsByPrefijoGuia(String prefijoGuia);

    boolean existsByPrefijoGuiaAndIdProveedorNot(
            String prefijoGuia,
            Integer idProveedor
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proveedor p where p.idProveedor = :id")
    Optional<Proveedor> findByIdForUpdate(@Param("id") Integer id);
}