package com.intiwasi.backend.repository;

import com.intiwasi.backend.entity.Ajuste;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AjusteRepository extends JpaRepository<Ajuste, Integer> {
    @EntityGraph(attributePaths = {"documento", "documento.usuario"})
    @Query("select a from Ajuste a order by a.idAjuste desc")
    List<Ajuste> findAllConRelaciones();

    @EntityGraph(attributePaths = {"documento", "documento.usuario"})
    @Query("select a from Ajuste a where a.idAjuste = :id")
    Optional<Ajuste> findByIdConRelaciones(@Param("id") Integer id);
}
