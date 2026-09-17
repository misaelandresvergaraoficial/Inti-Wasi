package com.intiwasi.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.intiwasi.backend.entity.Proveedor;
public interface ProveedorRepository extends JpaRepository <Proveedor, Integer> {
    List<Proveedor> findByEstado(Integer estado);
}
