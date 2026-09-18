package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.Categoria.CategoriaRequest;
import com.intiwasi.backend.dto.Producto.ProductoRequest;
import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.dto.UsuarioRequest;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import com.intiwasi.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceExceptionClassificationTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void categoriaDuplicadaLanzaConflicto() {
        CategoriaRequest request = new CategoriaRequest();
        request.setNomCategoria("Granos");
        when(categoriaRepository.existsByNomCategoria("Granos")).thenReturn(true);

        assertThrows(
                ConflictoException.class,
                () -> new CategoriaService(categoriaRepository).registrar(request)
        );
    }

    @Test
    void productoDuplicadoLanzaConflicto() {
        ProductoRequest request = new ProductoRequest();
        request.setSku("SKU-001");
        when(productoRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThrows(
                ConflictoException.class,
                () -> new ProductoService(
                        productoRepository,
                        categoriaRepository,
                        proveedorRepository
                ).crearProducto(request)
        );
    }

    @Test
    void proveedorDuplicadoLanzaConflicto() {
        ProveedorRequest request = new ProveedorRequest();
        request.setNomProveedor("Proveedor existente");
        when(proveedorRepository.existsByNomProveedor("Proveedor existente")).thenReturn(true);

        assertThrows(
                ConflictoException.class,
                () -> new ProveedorService(proveedorRepository).registrar(request)
        );
    }

    @Test
    void usuarioDuplicadoLanzaConflicto() {
        UsuarioRequest request = new UsuarioRequest();
        request.setCorreo("existente@intiwasi.pe");
        when(usuarioRepository.existsByCorreo("existente@intiwasi.pe")).thenReturn(true);

        assertThrows(
                ConflictoException.class,
                () -> new UsuarioService(usuarioRepository, passwordEncoder).registrar(request)
        );
    }

    @Test
    void idInexistenteLanzaRecursoNoEncontrado() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> new CategoriaService(categoriaRepository).obtenerPorId(99)
        );
    }
}
