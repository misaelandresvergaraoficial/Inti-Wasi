package com.intiwasi.backend.dto;

import com.intiwasi.backend.dto.Categoria.CategoriaRequest;
import com.intiwasi.backend.dto.OrdenCompra.DetalleOrdenCompraRequest;
import com.intiwasi.backend.dto.Producto.ProductoRequest;
import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.dto.Usuario.UsuarioRequest;
import com.intiwasi.backend.dto.inventario.AjusteRequest;
import com.intiwasi.backend.dto.inventario.EntradaRequest;
import com.intiwasi.backend.dto.inventario.MovimientoRequest;
import com.intiwasi.backend.dto.inventario.SalidaRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que las reglas de sanitización de la Actividad 11 rechacen los
 * valores inválidos y sigan aceptando los datos reales del sistema.
 */
class ValidacionSanitizacionTest {

    private static final Validator VALIDADOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    // --- Usuarios -------------------------------------------------------

    @Test
    @DisplayName("El nombre de usuario con números se rechaza")
    void nombreUsuarioConNumerosSeRechaza() {
        UsuarioRequest request = usuarioValido();
        request.setNomUsuario("Misael 2026");

        assertTrue(infringe(request, "nomUsuario"));
    }

    @Test
    @DisplayName("El nombre de usuario con emoji se rechaza")
    void nombreUsuarioConEmojiSeRechaza() {
        UsuarioRequest request = usuarioValido();
        request.setNomUsuario("Misael \uD83D\uDE00");

        assertTrue(infringe(request, "nomUsuario"));
    }

    @Test
    @DisplayName("El nombre real con tildes se acepta")
    void nombreUsuarioConTildesSeAcepta() {
        UsuarioRequest request = usuarioValido();
        request.setNomUsuario("Misael Andrés Vergara Morales");

        assertFalse(infringe(request, "nomUsuario"));
    }

    @Test
    @DisplayName("El teléfono de usuario con 8 o 10 dígitos se rechaza")
    void telefonoUsuarioConLongitudIncorrectaSeRechaza() {
        UsuarioRequest ocho = usuarioValido();
        ocho.setTelefono("98765432");
        UsuarioRequest diez = usuarioValido();
        diez.setTelefono("9876543210");

        assertTrue(infringe(ocho, "telefono"));
        assertTrue(infringe(diez, "telefono"));
    }

    @Test
    @DisplayName("El teléfono de usuario que no empieza con 9 se rechaza")
    void telefonoUsuarioSinPrefijoNueveSeRechaza() {
        UsuarioRequest request = usuarioValido();
        request.setTelefono("879348552");

        assertTrue(infringe(request, "telefono"));
    }

    @Test
    @DisplayName("El teléfono de usuario de 9 dígitos que empieza con 9 se acepta")
    void telefonoUsuarioValidoSeAcepta() {
        UsuarioRequest request = usuarioValido();
        request.setTelefono("979348552");

        assertFalse(infringe(request, "telefono"));
    }

    // --- Proveedores ----------------------------------------------------

    @Test
    @DisplayName("La razón social admite números, pero el contacto no")
    void razonSocialAdmiteNumerosYContactoNo() {
        ProveedorRequest request = proveedorValido();
        request.setNomProveedor("Distribuidora 2026 S.A.C.");
        request.setContacto("Jorge 2");

        assertFalse(infringe(request, "nomProveedor"));
        assertTrue(infringe(request, "contacto"));
    }

    @Test
    @DisplayName("El contacto con números o signos se rechaza")
    void contactoConNumerosSeRechaza() {
        ProveedorRequest request = proveedorValido();
        request.setContacto("Jorge 2");

        assertTrue(infringe(request, "contacto"));
    }

    @Test
    @DisplayName("La razón social y el contacto reales con tildes y puntos se aceptan")
    void proveedorConTildesSeAcepta() {
        ProveedorRequest request = proveedorValido();
        request.setNomProveedor("Ingram Micro Perú S.A.C.");
        request.setContacto("Mariana Falcón");

        assertFalse(infringe(request, "nomProveedor"));
        assertFalse(infringe(request, "contacto"));
    }

    @Test
    @DisplayName("El teléfono de proveedor acepta celular y fijo, y rechaza 8 o 10 dígitos")
    void telefonoProveedorValidaCelularYFijo() {
        ProveedorRequest celular = proveedorValido();
        celular.setTelefono("979348552");
        ProveedorRequest fijo = proveedorValido();
        fijo.setTelefono("014150000");
        ProveedorRequest ocho = proveedorValido();
        ocho.setTelefono("01415000");
        ProveedorRequest diez = proveedorValido();
        diez.setTelefono("0141500001");

        assertFalse(infringe(celular, "telefono"));
        assertFalse(infringe(fijo, "telefono"));
        assertTrue(infringe(ocho, "telefono"));
        assertTrue(infringe(diez, "telefono"));
    }

    // --- Categorías -----------------------------------------------------

    @Test
    @DisplayName("La categoría con emoji o guion bajo se rechaza")
    void categoriaConEmojiSeRechaza() {
        CategoriaRequest emoji = categoriaValida();
        emoji.setNomCategoria("Periféricos \u2705");
        CategoriaRequest guionBajo = categoriaValida();
        guionBajo.setNomCategoria("Memorias_RAM");

        assertTrue(infringe(emoji, "nomCategoria"));
        assertTrue(infringe(guionBajo, "nomCategoria"));
    }

    @Test
    @DisplayName("La categoría real con paréntesis y barra se acepta")
    void categoriaRealSeAcepta() {
        CategoriaRequest request = categoriaValida();
        request.setNomCategoria("Almacenamiento (SSD / HDD)");

        assertFalse(infringe(request, "nomCategoria"));
    }

    // --- Productos ------------------------------------------------------

    @Test
    @DisplayName("El producto con emoji se rechaza y con & se acepta")
    void productoValidaSimbolosPermitidos() {
        ProductoRequest emoji = productoValido();
        emoji.setNomProducto("Mouse Gamer \uD83D\uDDB1");
        ProductoRequest ampersand = productoValido();
        ampersand.setNomProducto("Cable HDMI 2.1 & Adaptador (3 m)");

        assertTrue(infringe(emoji, "nomProducto"));
        assertFalse(infringe(ampersand, "nomProducto"));
    }

    @Test
    @DisplayName("El precio negativo se rechaza y el cero se acepta")
    void precioNegativoSeRechazaYCeroSeAcepta() {
        ProductoRequest negativo = productoValido();
        negativo.setPrecio(new BigDecimal("-1"));
        ProductoRequest cero = productoValido();
        cero.setPrecio(BigDecimal.ZERO);

        assertTrue(infringe(negativo, "precio"));
        assertFalse(infringe(cero, "precio"));
    }

    @Test
    @DisplayName("El stock mínimo negativo se rechaza y el cero se acepta")
    void stockMinimoNegativoSeRechaza() {
        ProductoRequest negativo = productoValido();
        negativo.setStockMinimo(-1);
        ProductoRequest cero = productoValido();
        cero.setStockMinimo(0);

        assertTrue(infringe(negativo, "stockMinimo"));
        assertFalse(infringe(cero, "stockMinimo"));
    }

    // --- Operaciones ----------------------------------------------------

    @Test
    @DisplayName("La cantidad negativa o cero se rechaza en movimientos y detalles")
    void cantidadNoPositivaSeRechaza() {
        MovimientoRequest movimiento = new MovimientoRequest();
        movimiento.setIdProducto(1);
        movimiento.setCantidad(-5);

        DetalleOrdenCompraRequest detalle = new DetalleOrdenCompraRequest();
        detalle.setIdProducto(1);
        detalle.setCantidad(-5);
        detalle.setPrecioUnitario(new BigDecimal("10.00"));

        assertTrue(infringe(movimiento, "cantidad"));
        assertTrue(infringe(detalle, "cantidad"));
    }

    @Test
    @DisplayName("El precio unitario negativo se rechaza y el cero se acepta")
    void precioUnitarioNegativoSeRechaza() {
        DetalleOrdenCompraRequest negativo = new DetalleOrdenCompraRequest();
        negativo.setIdProducto(1);
        negativo.setCantidad(1);
        negativo.setPrecioUnitario(new BigDecimal("-1"));
        DetalleOrdenCompraRequest cero = new DetalleOrdenCompraRequest();
        cero.setIdProducto(1);
        cero.setCantidad(1);
        cero.setPrecioUnitario(BigDecimal.ZERO);

        assertTrue(infringe(negativo, "precioUnitario"));
        assertFalse(infringe(cero, "precioUnitario"));
    }

    @Test
    @DisplayName("Las observaciones con emoji se rechazan en entrada, salida y ajuste")
    void observacionesConEmojiSeRechazan() {
        EntradaRequest entrada = new EntradaRequest();
        entrada.setIdOrden(1);
        entrada.setNumeroGuiaRemision("GUIA-DELTRON-001");
        entrada.setObservaciones("Llegó completo \u2705");
        entrada.setMovimientos(List.of(movimientoValido()));

        SalidaRequest salida = new SalidaRequest();
        salida.setObservaciones("Entregado \uD83D\uDE80");
        salida.setMovimientos(List.of(movimientoValido()));

        AjusteRequest ajuste = new AjusteRequest();
        ajuste.setMotivo("Conteo físico \u26A0");
        ajuste.setMovimientos(List.of(movimientoValido()));

        assertTrue(infringe(entrada, "observaciones"));
        assertTrue(infringe(salida, "observaciones"));
        assertTrue(infringe(ajuste, "motivo"));
    }

    @Test
    @DisplayName("Las observaciones de texto normal se aceptan")
    void observacionesDeTextoSeAceptan() {
        EntradaRequest entrada = new EntradaRequest();
        entrada.setIdOrden(1);
        entrada.setNumeroGuiaRemision("GUIA-DELTRON-001");
        entrada.setObservaciones("Llegó completo; se revisó caja por caja.");
        entrada.setMovimientos(List.of(movimientoValido()));

        assertFalse(infringe(entrada, "observaciones"));
    }

    // --- Utilidades -----------------------------------------------------

    @Test
    @DisplayName("Los textos opcionales en blanco se normalizan a nulo")
    void blancosSeNormalizanANulo() {
        UsuarioRequest usuario = usuarioValido();
        usuario.setTelefono("");
        ProveedorRequest proveedor = proveedorValido();
        proveedor.setContacto("   ");
        EntradaRequest entrada = new EntradaRequest();
        entrada.setIdOrden(1);
        entrada.setNumeroGuiaRemision("GUIA-DELTRON-001");
        entrada.setObservaciones("  ");
        entrada.setMovimientos(List.of(movimientoValido()));

        assertNull(usuario.getTelefono());
        assertNull(proveedor.getContacto());
        assertNull(entrada.getObservaciones());
        assertTrue(VALIDADOR.validate(usuario).isEmpty());
        assertTrue(VALIDADOR.validate(proveedor).isEmpty());
        assertTrue(VALIDADOR.validate(entrada).isEmpty());
    }

    private static <T> boolean infringe(T bean, String campo) {
        Set<ConstraintViolation<T>> violaciones = VALIDADOR.validate(bean);
        Set<String> campos = violaciones.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        return campos.contains(campo);
    }

    private static MovimientoRequest movimientoValido() {
        MovimientoRequest movimiento = new MovimientoRequest();
        movimiento.setIdProducto(1);
        movimiento.setCantidad(1);
        return movimiento;
    }

    private static UsuarioRequest usuarioValido() {
        UsuarioRequest request = new UsuarioRequest();
        request.setNomUsuario("Misael Andrés");
        request.setCorreo("admin.misael@intiwasi.pe");
        request.setRol("Administrador");
        request.setTelefono("987654321");
        return request;
    }

    private static ProveedorRequest proveedorValido() {
        ProveedorRequest request = new ProveedorRequest();
        request.setNomProveedor("Grupo Deltron S.A.");
        request.setPrefijoGuia("GUIA-DELTRON-");
        request.setRuc("20212345678");
        request.setContacto("Jorge Del Solar");
        request.setTelefono("014150000");
        return request;
    }

    private static CategoriaRequest categoriaValida() {
        CategoriaRequest request = new CategoriaRequest();
        request.setNomCategoria("Procesadores");
        request.setEstado(1);
        return request;
    }

    private static ProductoRequest productoValido() {
        ProductoRequest request = new ProductoRequest();
        request.setSku("QA-PROD-01");
        request.setNomProducto("Procesador AMD Ryzen 5 5600X 3.7GHz 6-Core");
        request.setIdCategoria(1);
        request.setPrecio(new BigDecimal("680.00"));
        request.setStockMinimo(5);
        return request;
    }
}
