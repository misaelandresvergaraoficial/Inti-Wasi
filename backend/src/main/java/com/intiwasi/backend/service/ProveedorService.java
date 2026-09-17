package com.intiwasi.backend.service;

import java.util.List;

import com.intiwasi.backend.entity.Proveedor;

public interface ProveedorService {
    List<Proveedor> listarActivos();
    Proveedor obtenerPorId(Integer id);
    Proveedor registrar(Proveedor proveedor);
    Proveedor actualizar(Integer id, Proveedor proveedor);
    void desactivar(Integer id);
}
