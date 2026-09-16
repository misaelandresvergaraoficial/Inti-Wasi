package com.intiwasi.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.service.CategoriaService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
@RestController
@RequestMapping("/api/categorias") // Ruta base para este controlador
@CrossOrigin(origins = "*") // Permite peticiones desde el frontend (Angular, React, etc.)
public class CategoriaController {

    private final CategoriaService categoriaService;

    // 1. listarActivas() [GET]
    // Endpoint: GET /api/categorias
    @GetMapping
    public ResponseEntity<List<Categoria>> listarActivas() {
        // Retorna directamente el llamado al servicio
        return new ResponseEntity<>(categoriaService.listarActivas(), HttpStatus.OK); 
    }

    // 2. obtenerPorId(id) [GET]
    // Endpoint: GET /api/categorias/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable("id") Integer id) {
        try {
            Categoria categoria = categoriaService.obtenerPorId(id);
            return new ResponseEntity<>(categoria, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND); // Devuelve 404 si no existe
        }
    }

    // 3. registrar(request) [POST]
    // Endpoint: POST /api/categorias
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Categoria request) {
        try {
            Categoria nuevaCategoria = categoriaService.registrar(request);
            return new ResponseEntity<>(nuevaCategoria, HttpStatus.CREATED); // Devuelve 201 Created
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // Devuelve error si ya existe
        }
    }

    // 4. actualizar(id, request) [PUT]
    // Endpoint: PUT /api/categorias/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable("id") Integer id, @RequestBody Categoria request) {
        try {
            Categoria categoriaActualizada = categoriaService.actualizar(id, request);
            return new ResponseEntity<>(categoriaActualizada, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // 5. desactivar(id) [DELETE]
    // Endpoint: DELETE /api/categorias/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivar(@PathVariable("id") Integer id) {
        try {
            categoriaService.desactivar(id);
            return new ResponseEntity<>("Categoría desactivada exitosamente", HttpStatus.NO_CONTENT); // 204 No Content o un OK con mensaje
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}