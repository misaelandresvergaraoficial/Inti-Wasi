package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.dto.Proveedor.ProveedorResponse;
import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.GuiaRemisionRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final GuiaRemisionRepository guiaRepository;

    public List<ProveedorResponse> listarActivos() {
        return proveedorRepository.findByEstado((byte) 1).stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public List<ProveedorResponse> listarTodos() {
        return proveedorRepository.findAll().stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public ProveedorResponse obtenerPorId(Integer id) {
        return convertirAResponse(obtenerEntidadPorId(id));
    }

    @Transactional
    public ProveedorResponse registrar(ProveedorRequest request) {
        validarPrefijo(request.getPrefijoGuia());

        if (proveedorRepository.existsByPrefijoGuia(request.getPrefijoGuia())) {
            throw new ConflictoException(
                    "prefijoGuia: Este prefijo ya pertenece a otro proveedor."
            );
        }

        if (proveedorRepository.existsByNomProveedor(request.getNomProveedor())) {
            throw new ConflictoException(
                    "El nombre del proveedor ya se encuentra registrado"
            );
        }

        if (proveedorRepository.existsByRuc(request.getRuc())) {
            throw new ConflictoException(
                    "El RUC ya se encuentra registrado en el sistema"
            );
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setNomProveedor(request.getNomProveedor());
        proveedor.setRuc(request.getRuc());
        proveedor.setPrefijoGuia(request.getPrefijoGuia());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());
        proveedor.setEstado((byte) 1);

        return convertirAResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(Integer id, ProveedorRequest request) {
        validarPrefijo(request.getPrefijoGuia());

        Proveedor proveedorExistente = proveedorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Proveedor no encontrado con ID: " + id
                ));

        boolean cambiaPrefijo = !request.getPrefijoGuia().equals(
                proveedorExistente.getPrefijoGuia()
        );

        if (cambiaPrefijo
                && proveedorExistente.getPrefijoGuia() != null
                && guiaRepository.existsByProveedor_IdProveedor(id)) {
            throw new ReglaNegocioException(
                    "prefijoGuia: No se puede cambiar el prefijo porque el proveedor "
                            + "ya tiene guías registradas. El historial debe conservarse."
            );
        }

        if (proveedorRepository.existsByPrefijoGuiaAndIdProveedorNot(
                request.getPrefijoGuia(), id
        )) {
            throw new ConflictoException(
                    "prefijoGuia: Este prefijo ya pertenece a otro proveedor."
            );
        }

        if (!proveedorExistente.getNomProveedor().equalsIgnoreCase(
                request.getNomProveedor()
        ) && proveedorRepository.existsByNomProveedor(request.getNomProveedor())) {
            throw new ConflictoException(
                    "El nombre del proveedor ya se encuentra registrado por otro proveedor"
            );
        }

        if (!proveedorExistente.getRuc().equals(request.getRuc())
                && proveedorRepository.existsByRuc(request.getRuc())) {
            throw new ConflictoException(
                    "El RUC ya se encuentra registrado por otro proveedor"
            );
        }

        proveedorExistente.setNomProveedor(request.getNomProveedor());
        proveedorExistente.setRuc(request.getRuc());
        proveedorExistente.setPrefijoGuia(request.getPrefijoGuia());
        proveedorExistente.setContacto(request.getContacto());
        proveedorExistente.setTelefono(request.getTelefono());
        proveedorExistente.setDireccion(request.getDireccion());

        return convertirAResponse(proveedorRepository.save(proveedorExistente));
    }

    @Transactional
    public void desactivar(Integer id) {
        Proveedor proveedor = proveedorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Proveedor no encontrado con ID: " + id
                ));

        proveedor.setEstado((byte) 0);
        proveedorRepository.save(proveedor);
    }

    @Transactional
    public ProveedorResponse cambiarEstado(Integer id, Integer estado) {
        Proveedor proveedor = proveedorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Proveedor no encontrado con ID: " + id
                ));

        proveedor.setEstado(estado.byteValue());

        return convertirAResponse(proveedorRepository.save(proveedor));
    }

    private void validarPrefijo(String prefijo) {
        if (!GuiaRemisionFormato.prefijoValido(prefijo)) {
            throw new IllegalArgumentException(
                    "prefijoGuia: Usa GUIA-CODIGO-, en mayúsculas, con un código "
                            + "de 1 a 24 letras o números que empiece con una letra."
            );
        }
    }

    private Proveedor obtenerEntidadPorId(Integer id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Proveedor no encontrado con ID: " + id
                ));
    }

    private ProveedorResponse convertirAResponse(Proveedor proveedor) {
        ProveedorResponse response = new ProveedorResponse();
        response.setIdProveedor(proveedor.getIdProveedor());
        response.setNomProveedor(proveedor.getNomProveedor());
        response.setRuc(proveedor.getRuc());
        response.setPrefijoGuia(proveedor.getPrefijoGuia());
        response.setPrefijoGuiaBloqueado(
                proveedor.getPrefijoGuia() != null
                        && guiaRepository.existsByProveedor_IdProveedor(
                                proveedor.getIdProveedor()
                        )
        );
        response.setContacto(proveedor.getContacto());
        response.setTelefono(proveedor.getTelefono());
        response.setDireccion(proveedor.getDireccion());
        response.setEstado(proveedor.getEstado());
        return response;
    }
}