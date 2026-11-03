package com.intiwasi.backend.dto;

import com.intiwasi.backend.config.JsonConfig;
import com.intiwasi.backend.dto.Usuario.UsuarioRequest;
import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.dto.inventario.CorreccionRequest;
import com.intiwasi.backend.dto.inventario.EntradaRequest;
import com.intiwasi.backend.dto.inventario.MovimientoRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class ContratosEntradaTest {
    private JsonMapper mapper() {
        JsonConfig config = new JsonConfig();
        var factory = JsonFactory.builder();
        config.strictFactory().customize(factory);
        var builder = JsonMapper.builder(factory.build());
        config.strictMapper().customize(builder);
        return builder.build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "\"1\"", "true", "2147483648"})
    void cantidadesFueraDeContratoNoSeConviertenSilenciosamente(String quantity) {
        assertThrows(JacksonException.class, () -> mapper().readValue(
                "{\"idProducto\":1,\"cantidad\":" + quantity + "}", MovimientoRequest.class));
    }

    @Test
    void rechazaCamposDesconocidosDuplicadosYContenidoPosterior() {
        for (String body : new String[]{"{\"idProducto\":1,\"cantidad\":1,\"stockActual\":999}",
                "{\"idProducto\":1,\"cantidad\":1,\"cantidad\":2}",
                "{\"idProducto\":1,\"cantidad\":1} {}"}) {
            assertThrows(JacksonException.class, () -> mapper().readValue(body, MovimientoRequest.class));
        }
        assertEquals(1, mapper().readValue("{\"idProducto\":1,\"cantidad\":1}", MovimientoRequest.class).getCantidad());
    }

    @Test
    void normalizaNombresLegitimosSinCambiarLaContrasena() {
        UsuarioRequest user = new UsuarioRequest();
        user.setNomUsuario("  Mari\u0301a-José D’Angelo  ");
        user.setCorreo("operador.maria@intiwasi.pe");
        user.setRol("Operador de Almacén");
        user.setContrasena(" Abcdé ");
        ProveedorRequest supplier = new ProveedorRequest();
        supplier.setContacto("  Ana O'Neill  ");
        assertEquals("María-José D’Angelo", user.getNomUsuario());
        assertEquals("Ana O'Neill", supplier.getContacto());
        assertEquals(" Abcdé ", user.getContrasena());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(user).isEmpty());
            user.setNomUsuario("Ana \uD801\uDC00\uD801\uDC28");
            assertTrue(factory.getValidator().validate(user).isEmpty());
        }
    }

    @Test
    void admiteCienLineasYRechazaCientoUnaYEmojisEnCorrecciones() {
        EntradaRequest request = new EntradaRequest();
        request.setIdOrden(1);
        request.setNumeroGuiaRemision("GUIA-AND-001");
        request.setMovimientos(IntStream.rangeClosed(1,100).mapToObj(id -> {
            MovimientoRequest line = new MovimientoRequest();
            line.setIdProducto(id); line.setCantidad(1); return line;
        }).toList());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(request).isEmpty());
            var lines = new java.util.ArrayList<>(request.getMovimientos());
            lines.add(lines.getFirst()); request.setMovimientos(lines);
            assertFalse(validator.validate(request).isEmpty());
            CorreccionRequest correction = new CorreccionRequest();
            correction.setMotivo("Corrección 😀");
            assertFalse(validator.validate(correction).isEmpty());
            correction.setMotivo("Corrección por conteo \uD801\uDC00\uD801\uDC28");
            assertTrue(validator.validate(correction).isEmpty());
        }
    }
}
