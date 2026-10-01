package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.Producto.ProductoResponse;
import com.intiwasi.backend.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {
    @Mock ProductoService service;
    @InjectMocks ProductoController controller;

    @Test
    void operadorConsultaSoloActivos() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "operador", null, List.of(new SimpleGrantedAuthority("ROLE_Operador de Almacén")));
        var response = new ProductoResponse();
        when(service.obtenerPorId(12, false)).thenReturn(response);

        assertEquals(response, controller.obtenerPorId(12, authentication).getBody());
        verify(service).obtenerPorId(12, false);
    }

    @Test
    void administradorPuedeConsultarInactivos() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "administrador", null, List.of(new SimpleGrantedAuthority("ROLE_Administrador")));
        var response = new ProductoResponse();
        when(service.obtenerPorId(12, true)).thenReturn(response);

        assertEquals(response, controller.obtenerPorId(12, authentication).getBody());
        verify(service).obtenerPorId(12, true);
    }
}
