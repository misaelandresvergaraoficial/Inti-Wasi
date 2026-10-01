package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Salida;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalidaRepository extends JpaRepository<Salida, Integer> {
    @EntityGraph(attributePaths = {"documento", "documento.usuario"})
    @Query("select s from Salida s order by s.idSalida desc")
    List<Salida> findAllConRelaciones();

    @EntityGraph(attributePaths = {"documento", "documento.usuario"})
    @Query("select s from Salida s where s.idSalida = :id")
    Optional<Salida> findByIdConRelaciones(@Param("id") Integer id);
}
