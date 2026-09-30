package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.StockBajo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockBajoRepository extends Repository<StockBajo, Integer> {
    List<StockBajo> findTop20ByOrderByUnidadesPorReponerDesc();

    @Query("select s from StockBajo s where (:idProducto is null or s.idProducto = :idProducto)")
    Page<StockBajo> buscar(@Param("idProducto") Integer idProducto, Pageable pageable);
}
