package com.intiwasi.backend.service.impl;

import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.dto.Proveedor.ProveedorResponse;
import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.repository.ProveedorRepository;
import com.intiwasi.backend.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Override
    public List<ProveedorResponse> listarActivos() {
        return proveedorRepository.findByEstado((byte) 1).stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    public ProveedorResponse obtenerPorId(Integer id) {
        return convertirAResponse(obtenerEntidadPorId(id));
    }

    @Override
    public ProveedorResponse registrar(ProveedorRequest request) {
        if (proveedorRepository.existsByNomProveedor(request.getNomProveedor())) {
            throw new RuntimeException("El nombre del proveedor ya se encuentra registrado");
        }
        if (proveedorRepository.existsByRuc(request.getRuc())) {
            throw new RuntimeException("El RUC ya se encuentra registrado en el sistema");
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNomProveedor(request.getNomProveedor());
        proveedor.setRuc(request.getRuc());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setEstado((byte) 1);

        return convertirAResponse(proveedorRepository.save(proveedor));
    }

    @Override
    public ProveedorResponse actualizar(Integer id, ProveedorRequest request) {
        Proveedor proveedorExistente = obtenerEntidadPorId(id);

        if (!proveedorExistente.getNomProveedor().equalsIgnoreCase(request.getNomProveedor())
                && proveedorRepository.existsByNomProveedor(request.getNomProveedor())) {
            throw new RuntimeException("El nombre del proveedor ya se encuentra registrado por otro proveedor");
        }

        if (!proveedorExistente.getRuc().equals(request.getRuc())
                && proveedorRepository.existsByRuc(request.getRuc())) {
            throw new RuntimeException("El RUC ya se encuentra registrado por otro proveedor");
        }

        proveedorExistente.setNomProveedor(request.getNomProveedor());
        proveedorExistente.setRuc(request.getRuc());
        proveedorExistente.setContacto(request.getContacto());
        proveedorExistente.setTelefono(request.getTelefono());
        proveedorExistente.setDireccion(request.getDireccion());

        return convertirAResponse(proveedorRepository.save(proveedorExistente));
    }

    @Override
    public void desactivar(Integer id) {
        Proveedor proveedor = obtenerEntidadPorId(id);
        proveedor.setEstado((byte) 0);
        proveedorRepository.save(proveedor);
    }

    private Proveedor obtenerEntidadPorId(Integer id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + id));
    }

    private ProveedorResponse convertirAResponse(Proveedor proveedor) {
        ProveedorResponse response = new ProveedorResponse();
        response.setIdProveedor(proveedor.getIdProveedor());
        response.setNomProveedor(proveedor.getNomProveedor());
        response.setRuc(proveedor.getRuc());
        response.setContacto(proveedor.getContacto());
        response.setTelefono(proveedor.getTelefono());
        response.setDireccion(proveedor.getDireccion());
        response.setEstado(proveedor.getEstado());
        return response;
    }
}
