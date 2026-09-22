package com.intiwasi.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TipoDocumento {
    ENTRADA("Entrada"),
    SALIDA("Salida"),
    AJUSTE("Ajuste");

    private final String valor;

    TipoDocumento(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoDocumento desdeValor(String valor) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.valor.equalsIgnoreCase(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de documento inválido: " + valor));
    }
}
