package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.UsuarioRequest;
import com.intiwasi.backend.dto.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    List<UsuarioResponse> listarActivos();

    UsuarioResponse obtenerPorId(Integer id);

    UsuarioResponse registrar(UsuarioRequest request);

    UsuarioResponse actualizar(Integer id, UsuarioRequest request);

    void desactivar(Integer id);
}