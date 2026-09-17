package com.intiwasi.backend.service.impl;

import com.intiwasi.backend.dto.Categoria.CategoriaRequest;
import com.intiwasi.backend.dto.Categoria.CategoriaResponse;
import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Override
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByEstado((byte) 1).stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    public CategoriaResponse obtenerPorId(Integer idCategoria) {
        return convertirAResponse(obtenerEntidadPorId(idCategoria));
    }

    @Override
    public CategoriaResponse registrar(CategoriaRequest request) {
        if (categoriaRepository.existsByNomCategoria(request.getNomCategoria())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre.");
        }

        Categoria categoria = new Categoria();
        categoria.setNomCategoria(request.getNomCategoria());
        categoria.setEstado((byte) 1);

        return convertirAResponse(categoriaRepository.save(categoria));
    }

    @Override
    public CategoriaResponse actualizar(Integer idCategoria, CategoriaRequest request) {
        Categoria categoriaExistente = obtenerEntidadPorId(idCategoria);

        if (!categoriaExistente.getNomCategoria().equalsIgnoreCase(request.getNomCategoria())
                && categoriaRepository.existsByNomCategoria(request.getNomCategoria())) {
            throw new RuntimeException("El nombre de la categoría ya se encuentra registrado por otra categoría.");
        }

        categoriaExistente.setNomCategoria(request.getNomCategoria());
        return convertirAResponse(categoriaRepository.save(categoriaExistente));
    }

    @Override
    public void desactivar(Integer idCategoria) {
        Categoria categoriaExistente = obtenerEntidadPorId(idCategoria);
        categoriaExistente.setEstado((byte) 0);
        categoriaRepository.save(categoriaExistente);
    }

    private Categoria obtenerEntidadPorId(Integer idCategoria) {
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con el ID: " + idCategoria));
    }

    private CategoriaResponse convertirAResponse(Categoria categoria) {
        CategoriaResponse response = new CategoriaResponse();
        response.setIdCategoria(categoria.getIdCategoria());
        response.setNomCategoria(categoria.getNomCategoria());
        response.setEstado(categoria.getEstado());
        return response;
    }
}
