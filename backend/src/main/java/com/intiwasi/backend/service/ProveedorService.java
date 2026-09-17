package com.intiwasi.backend.service;

import java.util.List;

import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.dto.Proveedor.ProveedorResponse;

public interface ProveedorService {
    List<ProveedorResponse> listarActivos();
    ProveedorResponse obtenerPorId(Integer id);
    ProveedorResponse registrar(ProveedorRequest request);
    ProveedorResponse actualizar(Integer id, ProveedorRequest request);
    void desactivar(Integer id);
}
