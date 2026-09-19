package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.Producto.ProductoRequest;
import com.intiwasi.backend.dto.Producto.ProductoResponse;
import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodosActivos() {
        return productoRepository.findByEstado((byte) 1)
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductosStockBajo() {
        return productoRepository.obtenerProductosStockBajo()
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto no encontrado con el ID: " + id
                ));
        return convertirAResponse(producto);
    }

    @Transactional
    public ProductoResponse crearProducto(ProductoRequest request) {
        if (productoRepository.existsBySku(request.getSku())) {
            throw new ConflictoException("El SKU ya se encuentra registrado: " + request.getSku());
        }

        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoría no encontrada con ID: " + request.getIdCategoria()
                ));
        validarCategoriaActiva(categoria);

        Proveedor proveedor = null;
        if (request.getIdProveedor() != null) {
            proveedor = proveedorRepository.findById(request.getIdProveedor())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Proveedor no encontrado con ID: " + request.getIdProveedor()
                    ));
            validarProveedorActivo(proveedor);
        }

        Producto producto = Producto.builder()
                .sku(request.getSku())
                .nomProducto(request.getNomProducto())
                .categoria(categoria)
                .proveedor(proveedor)
                .precio(request.getPrecio())
                .stockMinimo(request.getStockMinimo())
                .stockActual(0)
                .estado((byte) 1)
                .build();

        Producto guardado = productoRepository.save(producto);
        return convertirAResponse(guardado);
    }

    @Transactional
    public ProductoResponse actualizarProducto(Integer id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto no encontrado con el ID: " + id
                ));

        if (!producto.getSku().equalsIgnoreCase(request.getSku()) && productoRepository.existsBySku(request.getSku())) {
            throw new ConflictoException("El SKU ingresado ya pertenece a otro producto.");
        }

        Categoria categoria = categoriaRepository.findById(request.getIdCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoría no encontrada con ID: " + request.getIdCategoria()
                ));
        validarCategoriaActiva(categoria);

        Proveedor proveedor = null;
        if (request.getIdProveedor() != null) {
            proveedor = proveedorRepository.findById(request.getIdProveedor())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Proveedor no encontrado con ID: " + request.getIdProveedor()
                    ));
            validarProveedorActivo(proveedor);
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

    @Transactional
    public void desactivarProducto(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto no encontrado con el ID: " + id
                ));
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

    private void validarCategoriaActiva(Categoria categoria) {
        if (!Byte.valueOf((byte) 1).equals(categoria.getEstado())) {
            throw new com.intiwasi.backend.exception.ReglaNegocioException(
                    "La categoría seleccionada está inactiva"
            );
        }
    }

    private void validarProveedorActivo(Proveedor proveedor) {
        if (!Byte.valueOf((byte) 1).equals(proveedor.getEstado())) {
            throw new com.intiwasi.backend.exception.ReglaNegocioException(
                    "El proveedor seleccionado está inactivo"
            );
        }
    }
}
