package com.intiwasi.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TipoAjuste {
    INCREMENTO("Incremento"),
    DECREMENTO("Decremento");

    private final String valor;

    TipoAjuste(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoAjuste desdeValor(String valor) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.valor.equalsIgnoreCase(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ajuste inválido: " + valor));
    }
}
