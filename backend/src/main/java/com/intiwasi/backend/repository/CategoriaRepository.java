package com.intiwasi.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intiwasi.backend.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    List<Categoria> findByEstado(Integer estado);
    boolean existsByNomCategoria(String nomCategoria);
    Optional<Categoria> findByNomCategoria(String nomCategoria);
}
