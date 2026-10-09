package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.Categoria.CategoriaRequest;
import com.intiwasi.backend.dto.Categoria.CategoriaResponse;
import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByEstado((byte) 1).stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll().stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Integer idCategoria) {
        return convertirAResponse(obtenerEntidadPorId(idCategoria));
    }

    @Transactional
    public CategoriaResponse registrar(CategoriaRequest request) {
        if (categoriaRepository.existsByNomCategoria(request.getNomCategoria())) {
            throw new ConflictoException("Ya existe una categoría con ese nombre.");
        }

        Categoria categoria = new Categoria();
        categoria.setNomCategoria(request.getNomCategoria());
        categoria.setEstado(request.getEstado().byteValue());

        return convertirAResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Integer idCategoria, CategoriaRequest request) {
        Categoria categoriaExistente = obtenerEntidadPorId(idCategoria);

        if (!categoriaExistente.getNomCategoria().equalsIgnoreCase(request.getNomCategoria())
                && categoriaRepository.existsByNomCategoria(request.getNomCategoria())) {
            throw new ConflictoException(
                    "El nombre de la categoría ya se encuentra registrado por otra categoría."
            );
        }

        categoriaExistente.setNomCategoria(request.getNomCategoria());
        Categoria actualizada = aplicarEstado(categoriaExistente, request.getEstado());
        return convertirAResponse(actualizada);
    }

    @Transactional
    public void desactivar(Integer idCategoria) {
        Categoria categoriaExistente = obtenerEntidadPorId(idCategoria);
        aplicarEstado(categoriaExistente, 0);
    }

    @Transactional
    public CategoriaResponse cambiarEstado(Integer idCategoria, Integer estado) {
        if (estado == null || (estado != 0 && estado != 1)) {
            throw new IllegalArgumentException("El estado de la categoría debe ser 0 o 1");
        }
        Categoria categoriaExistente = obtenerEntidadPorId(idCategoria);
        return convertirAResponse(aplicarEstado(categoriaExistente, estado));
    }

    /**
     * Aplica el nuevo estado a la categoría y, cuando se desactiva, propaga la
     * desactivación a todos los productos activos que pertenecen a ella, ya que
     * un producto no puede permanecer activo bajo una categoría inactiva.
     */
    private Categoria aplicarEstado(Categoria categoria, Integer nuevoEstado) {
        boolean seDesactiva = categoria.getEstado() == 1 && nuevoEstado == 0;

        categoria.setEstado(nuevoEstado.byteValue());
        Categoria guardada = categoriaRepository.save(categoria);

        if (seDesactiva) {
            List<Producto> productosAfectados = productoRepository
                    .findByCategoria_IdCategoriaAndEstado(guardada.getIdCategoria(), (byte) 1);
            productosAfectados.forEach(producto -> producto.setEstado((byte) 0));
            productoRepository.saveAll(productosAfectados);
        }

        return guardada;
    }

    private Categoria obtenerEntidadPorId(Integer idCategoria) {
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoría no encontrada con el ID: " + idCategoria
                ));
    }

    private CategoriaResponse convertirAResponse(Categoria categoria) {
        CategoriaResponse response = new CategoriaResponse();
        response.setIdCategoria(categoria.getIdCategoria());
        response.setNomCategoria(categoria.getNomCategoria());
        response.setEstado(categoria.getEstado());
        return response;
    }
}