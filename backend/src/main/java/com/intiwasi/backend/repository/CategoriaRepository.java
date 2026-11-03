package com.intiwasi.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intiwasi.backend.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Categoria c where c.idCategoria = :id")
    Optional<Categoria> findByIdForUpdate(@Param("id") Integer id);

    List<Categoria> findByEstado(Byte estado);
    boolean existsByNomCategoria(String nomCategoria);
    Optional<Categoria> findByNomCategoria(String nomCategoria);
}
