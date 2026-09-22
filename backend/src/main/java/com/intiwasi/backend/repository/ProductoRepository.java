package com.intiwasi.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.intiwasi.backend.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer>{

    // 1. Obtener solo los productos activos (Estado = 1) para el catálogo principal
    List<Producto> findByEstado(Byte estado);

    // 2. Buscar producto por SKU para validación de unicidad (RF06)
    Optional<Producto> findBySku(String sku);

    // 3. Verificar si un SKU ya existe en la BD antes de registrar o actualizar
    boolean existsBySku(String sku);

    // 4. Buscar productos por categoría activos
    List<Producto> findByCategoria_IdCategoriaAndEstado(Integer idCategoria, Byte estado);

    // 5. Consulta directa para productos con stock crítico (StockActual <= StockMinimo)
    // Alineado con el Dashboard y reporte de productos por reponer (RF13 / RF14)
    @Query("SELECT p FROM Producto p WHERE p.estado = 1 AND p.stockActual <= p.stockMinimo")
    List<Producto> obtenerProductosStockBajo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto IN :ids ORDER BY p.idProducto")
    List<Producto> findAllByIdForUpdate(@Param("ids") List<Integer> ids);

    long countByEstado(Byte estado);

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.estado = :estado AND p.stockActual <= p.stockMinimo")
    long countConStockBajo(@Param("estado") Byte estado);

    Page<Producto> findByEstado(Byte estado, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.estado = 1 AND (:idProducto IS NULL OR p.idProducto = :idProducto)")
    Page<Producto> buscarInventario(@Param("idProducto") Integer idProducto, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.estado = 1 AND p.stockActual <= p.stockMinimo")
    Page<Producto> obtenerProductosStockBajo(Pageable pageable);
}
