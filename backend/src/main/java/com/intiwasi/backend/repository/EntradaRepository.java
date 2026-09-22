package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Entrada;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntradaRepository extends JpaRepository<Entrada, Integer> {
    boolean existsByOrdenCompra_IdOrden(Integer idOrden);

    @EntityGraph(attributePaths = {"documento", "documento.usuario", "ordenCompra"})
    @Query("select e from Entrada e order by e.idEntrada desc")
    List<Entrada> findAllConRelaciones();

    @EntityGraph(attributePaths = {"documento", "documento.usuario", "ordenCompra"})
    @Query("select e from Entrada e where e.idEntrada = :id")
    Optional<Entrada> findByIdConRelaciones(@Param("id") Integer id);
}
