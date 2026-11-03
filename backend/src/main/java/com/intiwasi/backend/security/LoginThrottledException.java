package com.intiwasi.backend.security;

public class LoginThrottledException extends RuntimeException {
    private final int retryAfter;

    public LoginThrottledException(int retryAfter) {
        super("Se alcanzó el límite temporal de acceso. Espera unos momentos y vuelve a intentar.");
        this.retryAfter = retryAfter;
    }

    public int getRetryAfter() { return retryAfter; }
}
