package com.intiwasi.backend.exception;

public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }

    public ConflictoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
