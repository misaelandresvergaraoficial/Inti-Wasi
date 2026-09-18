package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.OrdenCompra;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Integer> {

    @EntityGraph(attributePaths = {"proveedor", "usuario", "detalles", "detalles.producto"})
    @Query("SELECT DISTINCT o FROM OrdenCompra o ORDER BY o.idOrden DESC")
    List<OrdenCompra> findAllConDetalles();

    @EntityGraph(attributePaths = {"proveedor", "usuario", "detalles", "detalles.producto"})
    @Query("SELECT o FROM OrdenCompra o WHERE o.idOrden = :id")
    Optional<OrdenCompra> findByIdConDetalles(@Param("id") Integer id);
}
