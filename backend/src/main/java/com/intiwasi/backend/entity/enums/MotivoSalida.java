package com.intiwasi.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MotivoSalida {
    DESPACHO_VENTA("Despacho/Venta"),
    MERMA("Merma"),
    OTRO("Otro");

    private final String valor;

    MotivoSalida(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static MotivoSalida desdeValor(String valor) {
        return Arrays.stream(values())
                .filter(motivo -> motivo.valor.equalsIgnoreCase(valor))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Motivo de salida inválido: " + valor));
    }
}
