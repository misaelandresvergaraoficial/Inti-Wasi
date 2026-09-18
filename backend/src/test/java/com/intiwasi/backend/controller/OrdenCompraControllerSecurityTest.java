package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraRequest;
import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraResponse;
import com.intiwasi.backend.service.OrdenCompraService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrdenCompraControllerSecurityTest {

    @Test
    void crearYActualizarSonExclusivosDelAdministrador() throws NoSuchMethodException {
        assertEquals("hasRole('Administrador')", autorizacion("crear", com.intiwasi.backend.dto.OrdenCompra.OrdenCompraRequest.class));
        assertEquals(
                "hasRole('Administrador')",
                autorizacion("actualizar", Integer.class, com.intiwasi.backend.dto.OrdenCompra.OrdenCompraRequest.class)
        );
    }

    @Test
    void operadorDeAlmacenPuedeConsultarOrdenes() throws NoSuchMethodException {
        String rolesEsperados = "hasAnyRole('Administrador', 'Operador de Almacén')";
        assertEquals(rolesEsperados, autorizacion("listarTodas"));
        assertEquals(rolesEsperados, autorizacion("obtenerPorId", Integer.class));
    }

    @Test
    void respuestasExitosasMantienenSusCodigosHttp() {
        OrdenCompraService service = mock(OrdenCompraService.class);
        OrdenCompraController controller = new OrdenCompraController(service);
        OrdenCompraRequest request = new OrdenCompraRequest();
        OrdenCompraResponse response = new OrdenCompraResponse();

        when(service.listarTodas()).thenReturn(List.of());
        when(service.crear(request)).thenReturn(response);
        when(service.actualizar(1, request)).thenReturn(response);

        assertEquals(HttpStatus.OK, controller.listarTodas().getStatusCode());
        assertEquals(HttpStatus.CREATED, controller.crear(request).getStatusCode());
        assertEquals(HttpStatus.OK, controller.actualizar(1, request).getStatusCode());
    }

    private String autorizacion(String metodo, Class<?>... parametros) throws NoSuchMethodException {
        Method endpoint = OrdenCompraController.class.getMethod(metodo, parametros);
        return endpoint.getAnnotation(PreAuthorize.class).value();
    }
}
