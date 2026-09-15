package com.intiwasi.backend.service;

import java.util.List;

import com.intiwasi.backend.dto.Producto.ProductoRequest;
import com.intiwasi.backend.dto.Producto.ProductoResponse;

public interface ProductoService {

    List<ProductoResponse> listarTodosActivos();
    List<ProductoResponse> listarProductosStockBajo();
    ProductoResponse obtenerPorId(Integer id);
    ProductoResponse crearProducto(ProductoRequest request);
    ProductoResponse actualizarProducto(Integer id, ProductoRequest request);
    void desactivarProducto(Integer id); // Borrado lógico
}
