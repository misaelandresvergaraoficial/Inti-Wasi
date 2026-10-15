package com.intiwasi.backend.service;

import com.intiwasi.backend.exception.ReglaNegocioException;

import java.util.regex.Pattern;

public final class GuiaRemisionFormato {

    private static final Pattern PREFIJO =
            Pattern.compile("^GUIA-[A-Z][A-Z0-9]{0,23}-$");

    private static final Pattern NUMERACION =
            Pattern.compile("^[0-9]+(?:-[0-9]+)*$");

    private GuiaRemisionFormato() {
    }

    public static boolean prefijoValido(String prefijo) {
        return prefijo != null && PREFIJO.matcher(prefijo).matches();
    }

    public static String validarNueva(String numero, String prefijo) {
        if (!prefijoValido(prefijo)) {
            throw new ReglaNegocioException(
                    "numeroGuiaRemision: El proveedor de la orden no tiene "
                            + "un prefijo de guía válido. Solicita al Administrador "
                            + "que lo configure."
            );
        }

        String guia = numero == null ? "" : numero.trim();

        if (guia.isEmpty() || guia.length() > 50) {
            throw new IllegalArgumentException(
                    "numeroGuiaRemision: Ingresa una guía de hasta 50 caracteres."
            );
        }

        if (!guia.startsWith(prefijo)) {
            throw new IllegalArgumentException(
                    "numeroGuiaRemision: La guía no corresponde al proveedor "
                            + "de la orden. Debe comenzar con " + prefijo
            );
        }

        String numeracion = guia.substring(prefijo.length());

        if (!NUMERACION.matcher(numeracion).matches()) {
            throw new IllegalArgumentException(
                    "numeroGuiaRemision: Después de " + prefijo
                            + " ingresa dígitos, opcionalmente separados por guiones; "
                            + "por ejemplo, 001-000123."
            );
        }

        return guia;
    }
}