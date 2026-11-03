package com.intiwasi.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {
    private final SessionRegistryTest.TestClock clock = new SessionRegistryTest.TestClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    @Test
    void retrasaIntentosProgresivamenteAunqueCambienLasMayusculasOElOrigen() {
        for (int i = 0; i < 5; i++) limiter.check("admin.prueba@intiwasi.pe", "127.0.0.1");
        assertThrows(LoginThrottledException.class, () -> limiter.check("ADMIN.PRUEBA@intiwasi.pe", "127.0.0.2"));
        clock.advance(Duration.ofSeconds(1));
        limiter.check("admin.prueba@intiwasi.pe", "127.0.0.2");
        assertEquals(2, assertThrows(LoginThrottledException.class,
                () -> limiter.check("admin.prueba@intiwasi.pe", "127.0.0.3")).getRetryAfter());
        clock.advance(Duration.ofSeconds(2));
        limiter.check("admin.prueba@intiwasi.pe", "127.0.0.2");
        limiter.success("admin.prueba@intiwasi.pe");
        assertDoesNotThrow(() -> limiter.check("admin.prueba@intiwasi.pe", "127.0.0.2"));
    }

    @Test
    void limitaOrigenAunqueRoteCuentasYSuVentanaSeRecupera() {
        for (int i = 0; i < 60; i++) limiter.check("intento" + i + "@intiwasi.pe", "127.0.0.1");
        assertThrows(LoginThrottledException.class, () -> limiter.check("otra@intiwasi.pe", "127.0.0.1"));
        clock.advance(Duration.ofMinutes(1));
        assertDoesNotThrow(() -> limiter.check("otra@intiwasi.pe", "127.0.0.1"));
    }
}
