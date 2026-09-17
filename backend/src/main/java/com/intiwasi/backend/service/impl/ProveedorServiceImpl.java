package com.intiwasi.backend.service.impl;

import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.repository.ProveedorRepository;
import com.intiwasi.backend.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorServiceImpl implements ProveedorService{

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Override
    public List<Proveedor> listarActivos() {
        return proveedorRepository.findByEstado(1);
    }

    @Override
    public Optional<Proveedor> obtenerPorId(Integer id) {
        return proveedorRepository.findById(id);
    }

    @Override
    public Proveedor registrar(Proveedor proveedor) {
        proveedor.setEstado(1); // Siempre empiezan activos
        return proveedorRepository.save(proveedor);
    }

    @Override
    public Proveedor actualizar(Integer id, Proveedor proveedorActualizado) {
        return proveedorRepository.findById(id).map(proveedor -> {
            proveedor.setNomProveedor(proveedorActualizado.getNomProveedor());
            proveedor.setRuc(proveedorActualizado.getRuc());
            proveedor.setContacto(proveedorActualizado.getContacto());
            proveedor.setTelefono(proveedorActualizado.getTelefono());
            proveedor.setDireccion(proveedorActualizado.getDireccion());
            // No actualizamos el estado aquí para evitar que lo activen/desactiven por error
            return proveedorRepository.save(proveedor);
        }).orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
    }

    @Override
    public void desactivar(Integer id) {
        proveedorRepository.findById(id).ifPresent(proveedor -> {
            proveedor.setEstado(0); // Borrado lógico
            proveedorRepository.save(proveedor);
        });
    }
}