package com.intiwasi.backend.service;

import com.intiwasi.backend.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiaRemisionFormatoTest {

    @Test
    void aceptaPrefijosEstables() {
        assertTrue(
                GuiaRemisionFormato.prefijoValido("GUIA-TECHDATA-")
        );
        assertFalse(
                GuiaRemisionFormato.prefijoValido("GUIA-techdata-")
        );
        assertFalse(
                GuiaRemisionFormato.prefijoValido("GUIA-TECH-DATA-")
        );
        assertFalse(GuiaRemisionFormato.prefijoValido(null));
    }

    @Test
    void conservaCerosYRecortaSoloEspaciosExteriores() {
        assertEquals(
                "GUIA-TECHDATA-002-000123",
                GuiaRemisionFormato.validarNueva(
                        " GUIA-TECHDATA-002-000123 ",
                        "GUIA-TECHDATA-"
                )
        );
    }

    @Test
    void rechazaOtroProveedor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GuiaRemisionFormato.validarNueva(
                        "GUIA-DELTRON-001-123",
                        "GUIA-TECHDATA-"
                )
        );
    }

    @Test
    void rechazaNumeracionVaciaYLetrasYGuionesDobles() {
        for (String numero : new String[]{
                "", "ABC", "001--123", "001-", "-123", "001 123"
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> GuiaRemisionFormato.validarNueva(
                            "GUIA-TEST-" + numero,
                            "GUIA-TEST-"
                    )
            );
        }
    }

    @Test
    void aceptaMismaNumeracionConPrefijosDiferentes() {
        assertEquals(
                "GUIA-DELTRON-001-123",
                GuiaRemisionFormato.validarNueva(
                        "GUIA-DELTRON-001-123",
                        "GUIA-DELTRON-"
                )
        );

        assertEquals(
                "GUIA-TECHDATA-001-123",
                GuiaRemisionFormato.validarNueva(
                        "GUIA-TECHDATA-001-123",
                        "GUIA-TECHDATA-"
                )
        );
    }

    @Test
    void rechazaProveedorSinConfiguracionYGuiaDemasiadoLarga() {
        assertThrows(
                ReglaNegocioException.class,
                () -> GuiaRemisionFormato.validarNueva(
                        "001-123",
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> GuiaRemisionFormato.validarNueva(
                        "GUIA-TEST-" + "1".repeat(50),
                        "GUIA-TEST-"
                )
        );
    }
}