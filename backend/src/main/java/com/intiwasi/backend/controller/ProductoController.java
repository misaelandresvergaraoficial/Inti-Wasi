package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.Producto.ProductoRequest;
import com.intiwasi.backend.dto.Producto.ProductoResponse;
import com.intiwasi.backend.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;
    
    @GetMapping("/todos")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<List<ProductoResponse>> listarTodos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ProductoResponse> cambiarEstado(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {
        Integer estado = body.get("estado");
        return ResponseEntity.ok(productoService.cambiarEstado(id, estado));
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<List<ProductoResponse>> listarTodosActivos() {
        return ResponseEntity.ok(productoService.listarTodosActivos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<ProductoResponse> obtenerPorId(@PathVariable Integer id, Authentication authentication) {
        boolean esAdministrador = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_Administrador".equals(authority.getAuthority()));
        return ResponseEntity.ok(productoService.obtenerPorId(id, esAdministrador));
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<List<ProductoResponse>> listarStockBajo() {
        return ResponseEntity.ok(productoService.listarProductosStockBajo());
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse nuevoProducto = productoService.crearProducto(request);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Integer id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    
}
