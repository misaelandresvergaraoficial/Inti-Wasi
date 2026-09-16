package com.intiwasi.backend.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.service.CategoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class CategoriaServiceIMPL implements CategoriaService {

    private CategoriaRepository categoriaRepository;

    @Override
    public List<Categoria> listarActivas() {
        // Asumimos que el estado 1 significa "Activo"
        return categoriaRepository.findByEstado(1);
    }

    @Override
    public Categoria obtenerPorId(Integer idCategoria) {
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con el ID: " + idCategoria));
    }

    @Override
    public Categoria registrar(Categoria categoria) {
        // Validación opcional usando el método de tu repositorio
        if (categoriaRepository.existsByNomCategoria(categoria.getNomCategoria())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre.");
        }
        
        // Nos aseguramos de que se guarde como activa por defecto
        categoria.setEstado(1); 
        return categoriaRepository.save(categoria);
    }

    @Override
    public Categoria actualizar(Integer idCategoria, Categoria request) {
        Categoria categoriaExistente = obtenerPorId(idCategoria);
        
        // Actualizamos los campos permitidos
        categoriaExistente.setNomCategoria(request.getNomCategoria());
        // Si necesitas actualizar el estado explícitamente aquí, puedes agregarlo
        
        return categoriaRepository.save(categoriaExistente);
    }

    @Override
    public void desactivar(Integer idCategoria) {
        Categoria categoriaExistente = obtenerPorId(idCategoria);
        
        // Eliminación lógica: cambiamos el estado a 0 (Inactivo)
        categoriaExistente.setEstado(0);
        categoriaRepository.save(categoriaExistente);
    }
}