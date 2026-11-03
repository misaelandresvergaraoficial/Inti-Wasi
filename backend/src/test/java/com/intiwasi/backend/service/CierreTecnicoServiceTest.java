package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.Usuario.UsuarioRequest;
import com.intiwasi.backend.dto.inventario.MovimientoConsultaResponse;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.UsuarioRepository;
import com.intiwasi.backend.security.SessionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CierreTecnicoServiceTest {
    @Mock MovimientoConsultaService movimientos;
    @Mock UsuarioRepository usuarios;
    @Mock PasswordEncoder encoder;
    @Mock SessionRegistry sesiones;
    @InjectMocks ReporteService reportes;
    @InjectMocks UsuarioService usuarioService;

    private MovimientoConsultaResponse movimientoNegativo() {
        return MovimientoConsultaResponse.builder()
                .fechaEmision(LocalDateTime.of(2026, 11, 2, 10, 0))
                .tipoDocumento(TipoDocumento.SALIDA).sku("QA-NEGATIVO")
                .nomProducto("Producto de prueba").cantidadConSigno(-3)
                .motivo("Merma").usuarioResponsable("Persona ficticia").build();
    }

    @Test
    void csvConservaCantidadNegativaComoNumeroSinPrefijoDeFormula() {
        when(movimientos.consultar(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(movimientoNegativo()), call.getArgument(5), 1));

        String csv = new String(reportes.exportar("movimientos", "csv", null, null, null, null, null)
                .orElseThrow(), StandardCharsets.UTF_8);

        assertTrue(csv.contains(",\"-3\","));
        assertFalse(csv.contains("\"'-3\""));
        assertEquals(2, csv.lines().count());
    }

    @Test
    void exportaExactamenteDiezMilRegistrosReuniendoCincuentaPaginas() {
        when(movimientos.consultar(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(Collections.nCopies(200, movimientoNegativo()),
                        call.getArgument(5), 10_000));

        String csv = new String(reportes.exportar("movimientos", "csv", null, null, null, null, null)
                .orElseThrow(), StandardCharsets.UTF_8);

        assertEquals(10_001, csv.lines().count());
        assertEquals(10_000, csv.lines().filter(linea -> linea.contains("\"QA-NEGATIVO\"")).count());
        var paginas = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(movimientos, times(50)).consultar(isNull(), isNull(), isNull(), isNull(), isNull(), paginas.capture());
        for (int indice = 0; indice < 50; indice++) {
            assertEquals(indice, paginas.getAllValues().get(indice).getPageNumber());
            assertEquals(200, paginas.getAllValues().get(indice).getPageSize());
        }
    }

    @Test
    void ultimoAdministradorPresentaAdvertenciaCompletaAlDesactivarYCambiarRol() {
        Usuario administrador = new Usuario();
        administrador.setIdUsuario(1);
        administrador.setRol("Administrador");
        administrador.setEstado(1);
        when(usuarios.bloquearUsuarios()).thenReturn(List.of(administrador));
        String advertencia = "Siempre debe haber al menos un administrador activo. "
                + "Activa o asigna otro administrador antes de realizar este cambio.";
        UsuarioRequest solicitud = new UsuarioRequest();
        solicitud.setNomUsuario("Persona ficticia");
        solicitud.setCorreo("operador.prueba@intiwasi.pe");
        solicitud.setRol("Operador de Almacén");

        assertEquals(advertencia, assertThrows(ReglaNegocioException.class,
                () -> usuarioService.desactivar(1)).getMessage());
        assertEquals(advertencia, assertThrows(ReglaNegocioException.class,
                () -> usuarioService.actualizar(1, solicitud)).getMessage());
        assertEquals(1, administrador.getEstado());
        assertEquals("Administrador", administrador.getRol());
        verify(usuarios, never()).save(any());
        verifyNoInteractions(sesiones);
    }
}
