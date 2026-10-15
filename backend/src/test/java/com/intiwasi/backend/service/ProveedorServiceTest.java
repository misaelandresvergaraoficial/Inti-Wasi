package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.GuiaRemisionRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock ProveedorRepository proveedores;
    @Mock GuiaRemisionRepository guias;
    @InjectMocks ProveedorService service;

    private Proveedor proveedor(String prefijo) {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(3);
        proveedor.setNomProveedor("Tech Data");
        proveedor.setRuc("20123456789");
        proveedor.setPrefijoGuia(prefijo);
        return proveedor;
    }

    private ProveedorRequest datos(String prefijo) {
        ProveedorRequest request = new ProveedorRequest();
        request.setNomProveedor("Tech Data");
        request.setRuc("20123456789");
        request.setTelefono("123456789");
        request.setPrefijoGuia(prefijo);
        return request;
    }

    @Test
    void permitePrimeraConfiguracionSinRenombrarGuiasHistoricas() {
        Proveedor proveedor = proveedor(null);

        when(proveedores.findByIdForUpdate(3))
                .thenReturn(Optional.of(proveedor));
        when(proveedores.save(proveedor)).thenReturn(proveedor);
        when(guias.existsByProveedor_IdProveedor(3)).thenReturn(true);

        var resultado = service.actualizar(
                3, datos("GUIA-TECHDATA-")
        );

        assertEquals("GUIA-TECHDATA-", resultado.getPrefijoGuia());
        assertTrue(resultado.isPrefijoGuiaBloqueado());
        verify(guias, never()).save(any());
    }

    @Test
    void impideCambiarPrefijoConfiguradoConGuiasRegistradas() {
        Proveedor proveedor = proveedor("GUIA-TECHDATA-");

        when(proveedores.findByIdForUpdate(3))
                .thenReturn(Optional.of(proveedor));
        when(guias.existsByProveedor_IdProveedor(3)).thenReturn(true);

        assertThrows(
                ReglaNegocioException.class,
                () -> service.actualizar(3, datos("GUIA-OTRO-"))
        );

        verify(proveedores, never()).save(any());
        assertEquals("GUIA-TECHDATA-", proveedor.getPrefijoGuia());
    }

    @Test
    void permiteEditarContactoConservandoElPrefijoBloqueado() {
        Proveedor proveedor = proveedor("GUIA-TECHDATA-");

        when(proveedores.findByIdForUpdate(3))
                .thenReturn(Optional.of(proveedor));
        when(proveedores.save(proveedor)).thenReturn(proveedor);
        when(guias.existsByProveedor_IdProveedor(3)).thenReturn(true);

        var resultado = service.actualizar(
                3, datos("GUIA-TECHDATA-")
        );

        assertEquals("GUIA-TECHDATA-", resultado.getPrefijoGuia());
        verify(proveedores).save(proveedor);
    }

    @Test
    void rechazaPrefijoDeOtroProveedor() {
        Proveedor proveedor = proveedor(null);

        when(proveedores.findByIdForUpdate(3))
                .thenReturn(Optional.of(proveedor));

        when(proveedores.existsByPrefijoGuiaAndIdProveedorNot(
                "GUIA-DELTRON-", 3
        )).thenReturn(true);

        assertThrows(
                ConflictoException.class,
                () -> service.actualizar(3, datos("GUIA-DELTRON-"))
        );

        verify(proveedores, never()).save(any());
    }
}