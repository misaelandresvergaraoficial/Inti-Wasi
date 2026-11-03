package com.intiwasi.backend.dto.Comun;
public final class Patrones {

    public static final String NOMBRE_PERSONAL = "^[\\p{L}\\p{M}]+(?:[ '\u2019-][\\p{L}\\p{M}]+)*$";

    public static String normalizarNombre(String value) {
        return value == null ? null : java.text.Normalizer.normalize(value.strip(), java.text.Normalizer.Form.NFC);
    }

    private Patrones() {
    }
    public static final String SIN_EMOJIS =
            "^[^\\u2300-\\u23FF\\u25A0-\\u25FF\\u2600-\\u27BF\\u2B00-\\u2BFF\\u3030\\u303D\\u3297\\u3299\\u200D\\u20E3\\uFE0F\\x{1F000}-\\x{1FAFF}\\x{E0020}-\\x{E007F}\\p{Cs}]*$";
    public static final String RAZON_SOCIAL =
            "^[\\p{L}\\p{M}0-9 .,&'’\\-()/]{2,100}$";
}
