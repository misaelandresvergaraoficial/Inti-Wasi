package com.intiwasi.backend.service.impl;

import com.intiwasi.backend.dto.Producto.ProductoRequest;
import com.intiwasi.backend.dto.Producto.ProductoResponse;
import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import com.intiwasi.backend.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors; 

@Service 
public class ProductoServiceIMPL {

    private ProductoRepository productoRepository;

    private CategoriaRepository categoriaRepository;

    private ProveedorRepository proveedorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodosActivos() {
        return productoRepository.findByEstado((byte) 1)
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductosStockBajo() {
        return productoRepository.obtenerProductosStockBajo()
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con el ID: " + id));
        return convertirAResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse crearProducto(ProductoRequest request) {
        if (productoRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("El SKU ya se encuentra registrado: " + request.getSku());
        }

        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + request.getIdCategoria()));

        Proveedor proveedor = null;
        if (request.getIdProveedor() != null) {
            proveedor = proveedorRepository.findById(request.getIdProveedor())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + request.getIdProveedor()));
        }

        Producto producto = Producto.builder()
                .sku(request.getSku())
                .nomProducto(request.getNomProducto())
                .categoria(categoria)
                .proveedor(proveedor)
                .precio(request.getPrecio())
                .stockMinimo(request.getStockMinimo())
                .stockActual(0) // Inicia en 0, los triggers lo actualizan con los movimientos
                .estado((byte) 1)
                .build();

        Producto guardado = productoRepository.save(producto);
        return convertirAResponse(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse actualizarProducto(Integer id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con el ID: " + id));

        if (!producto.getSku().equalsIgnoreCase(request.getSku()) && productoRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("El SKU ingresado ya pertenece a otro producto.");
        }

        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + request.getIdCategoria()));

        Proveedor proveedor = null;
        if (request.getIdProveedor() != null) {
            proveedor = proveedorRepository.findById(request.getIdProveedor())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + request.getIdProveedor()));
        }

        producto.setSku(request.getSku());
        producto.setNomProducto(request.getNomProducto());
        producto.setCategoria(categoria);
        producto.setProveedor(proveedor);
        producto.setPrecio(request.getPrecio());
        producto.setStockMinimo(request.getStockMinimo());

        Producto actualizado = productoRepository.save(producto);
        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public void desactivarProducto(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con el ID: " + id));
        producto.setEstado((byte) 0);
        productoRepository.save(producto);
    }

    private ProductoResponse convertirAResponse(Producto producto) {
        ProductoResponse response = new ProductoResponse();
        response.setIdProducto(producto.getIdProducto());
        response.setSku(producto.getSku());
        response.setNomProducto(producto.getNomProducto());

        if (producto.getCategoria() != null) {
            response.setIdCategoria(producto.getCategoria().getIdCategoria());
            response.setNomCategoria(producto.getCategoria().getNomCategoria());
        }

        if (producto.getProveedor() != null) {
            response.setIdProveedor(producto.getProveedor().getIdProveedor());
            response.setNomProveedor(producto.getProveedor().getNomProveedor());
        }

        response.setPrecio(producto.getPrecio());
        response.setStockMinimo(producto.getStockMinimo());
        response.setStockActual(producto.getStockActual());
        response.setEstado(producto.getEstado());
        return response;
    }
    
}
