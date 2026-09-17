package com.intiwasi.backend.service.impl;

import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.repository.ProveedorRepository;
import com.intiwasi.backend.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    // Al usar @RequiredArgsConstructor, ya no necesitamos el @Autowired
    private final ProveedorRepository proveedorRepository;

    @Override
    public List<Proveedor> listarActivos() {
        return proveedorRepository.findByEstado(1);
    }

    @Override
    public Proveedor obtenerPorId(Integer id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
    }
    @Override
    public Proveedor registrar(Proveedor request) {
        // Validaciones de creación
        if (request.getNomProveedor() == null || request.getNomProveedor().isBlank()) {
            throw new RuntimeException("El nombre del proveedor es obligatorio");
        }
        if (proveedorRepository.existsByNomProveedor(request.getNomProveedor())) {
            throw new RuntimeException("El nombre del proveedor ya se encuentra registrado");
        }
        
        // Validación exacta de 11 dígitos y que sean solo números
        if (request.getRuc() == null || !request.getRuc().matches("^[0-9]{11}$")) {
            throw new RuntimeException("El RUC debe tener 11 dígitos numéricos exactos");
        }
        if (proveedorRepository.existsByRuc(request.getRuc())) {
            throw new RuntimeException("El RUC ya se encuentra registrado en el sistema");
        }

        request.setEstado(1);
        return proveedorRepository.save(request);
    }

    @Override
    public Proveedor actualizar(Integer id, Proveedor request) {
        Proveedor proveedorExistente = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));

        // Validar si están intentando poner un nombre que ya tiene otro proveedor
        if (!proveedorExistente.getNomProveedor().equalsIgnoreCase(request.getNomProveedor()) &&
                proveedorRepository.existsByNomProveedor(request.getNomProveedor())) {
            throw new RuntimeException("El nombre del proveedor ya se encuentra registrado por otro proveedor");
        }

        // Validación exacta de 11 dígitos para el RUC
        if (request.getRuc() == null || !request.getRuc().matches("^[0-9]{11}$")) {
            throw new RuntimeException("El RUC debe tener 11 dígitos numéricos exactos");
        }

        // Validar si están intentando poner un RUC que ya tiene otro proveedor
        if (!proveedorExistente.getRuc().equals(request.getRuc()) &&
                proveedorRepository.existsByRuc(request.getRuc())) {
            throw new RuntimeException("El RUC ya se encuentra registrado por otro proveedor");
        }

        proveedorExistente.setNomProveedor(request.getNomProveedor());
        proveedorExistente.setRuc(request.getRuc());
        proveedorExistente.setContacto(request.getContacto());
        proveedorExistente.setTelefono(request.getTelefono());
        proveedorExistente.setDireccion(request.getDireccion());

        return proveedorRepository.save(proveedorExistente);
    }

    @Override
    public void desactivar(Integer id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
        proveedor.setEstado(0);
        proveedorRepository.save(proveedor);
    }
}