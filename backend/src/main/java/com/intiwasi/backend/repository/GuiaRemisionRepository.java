package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.GuiaRemision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuiaRemisionRepository extends JpaRepository<GuiaRemision, Integer> {
    boolean existsByProveedor_IdProveedorAndNumeroGuia(Integer idProveedor, String numeroGuia);
}
