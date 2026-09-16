package com.intiwasi.backend.service;

import java.util.List;
import com.intiwasi.backend.entity.Categoria;

public interface CategoriaService {
    List<Categoria> listarActivas();
    Categoria obtenerPorId(Integer idCategoria);
    Categoria registrar(Categoria categoria);
    Categoria actualizar(Integer idCategoria, Categoria categoria);
    void desactivar(Integer idCategoria);
}