package com.intiwasi.backend.dto.Comun;

/**
 * Expresiones regulares de sanitización compartidas por los DTO.
 * Centralizan las reglas para que un cambio se aplique en un solo sitio.
 */
public final class Patrones {

    private Patrones() {
    }

    /**
     * Bloquea emojis BMP habituales (símbolos, flechas decorativas, dingbats)
     * y todo el plano astral (pares sustitutos), incluidos el selector de
     * variación y el conector de secuencias ZWJ.
     */
    public static final String SIN_EMOJIS =
            "^[^\\u2300-\\u23FF\\u25A0-\\u25FF\\u2600-\\u27BF\\u2B00-\\u2BFF\\u3030\\u303D\\u3297\\u3299\\u200D\\u20E3\\uFE0F\\uD83C-\\uDBFF\\uDC00-\\uDFFF]*$";

    /** Razón social y categorías: letras, dígitos, espacios y . , & - ( ) /. */
    public static final String RAZON_SOCIAL =
            "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,&\\-()/]{2,100}$";
}
