package com.intiwasi.backend.service;

import java.util.List;

import com.intiwasi.backend.dto.Categoria.CategoriaRequest;
import com.intiwasi.backend.dto.Categoria.CategoriaResponse;

public interface CategoriaService {
    List<CategoriaResponse> listarActivas();
    CategoriaResponse obtenerPorId(Integer idCategoria);
    CategoriaResponse registrar(CategoriaRequest request);
    CategoriaResponse actualizar(Integer idCategoria, CategoriaRequest request);
    void desactivar(Integer idCategoria);
}
